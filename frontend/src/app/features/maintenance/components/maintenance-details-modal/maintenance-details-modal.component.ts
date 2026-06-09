import { DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceMaterialUsed, MaintenanceRecordPerson } from '../../models/maintenance-detail.model';
import { MaintenanceMaterialOption, MaintenancePersonOption } from '../../models/maintenance-reference.model';
import { MaintenanceRecordSummary } from '../../models/maintenance-record.model';
import { MaintenanceDetailService } from '../../services/maintenance-detail.service';

type DeleteTargetType = 'person' | 'material';

interface DeleteTarget {
  type: DeleteTargetType;
  id: string;
  name: string;
}

@Component({
  selector: 'app-maintenance-details-modal',
  standalone: true,
  imports: [DecimalPipe, FormsModule, ReactiveFormsModule, TranslatePipe, ConfirmModalComponent],
  templateUrl: './maintenance-details-modal.component.html'
})
export class MaintenanceDetailsModalComponent implements OnChanges {
  private readonly maintenanceDetailService = inject(MaintenanceDetailService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() maintenanceRecord: MaintenanceRecordSummary | null = null;
  @Input() peopleOptions: MaintenancePersonOption[] = [];
  @Input() materialOptions: MaintenanceMaterialOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() detailsChanged = new EventEmitter<void>();

  readonly people = signal<MaintenanceRecordPerson[]>([]);
  readonly materials = signal<MaintenanceMaterialUsed[]>([]);

  readonly loading = signal(false);
  readonly addingPerson = signal(false);
  readonly savingMaterial = signal(false);
  readonly deleting = signal(false);
  readonly deleteTarget = signal<DeleteTarget | null>(null);
  readonly editingMaterial = signal<MaintenanceMaterialUsed | null>(null);

  readonly personForm = this.formBuilder.nonNullable.group({
    maintenancePersonId: ['', [Validators.required]]
  });

  readonly materialForm = this.formBuilder.nonNullable.group({
    materialId: [''],
    materialNameSnapshot: [''],
    quantity: ['', [Validators.min(0.01)]],
    unit: [''],
    notes: ['']
  });

  readonly deleteMessage = computed(() => {
    const target = this.deleteTarget();

    if (!target) {
      return '';
    }

    const key = target.type === 'person'
      ? 'maintenance.details.confirmRemovePersonMessage'
      : 'maintenance.details.confirmRemoveMaterialMessage';

    return this.languageService.instant(key, { name: target.name });
  });

  readonly availablePeople = computed(() => {
    const assignedIds = new Set(this.people().map((person) => person.maintenancePersonId));
    return this.peopleOptions.filter((person) => !assignedIds.has(person.id));
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['maintenanceRecord']) && this.open && this.maintenanceRecord) {
      this.loadDetails();
    }
  }

  requestClose(): void {
    if (this.loading() || this.addingPerson() || this.savingMaterial() || this.deleting()) {
      return;
    }

    this.resetState();
    this.close.emit();
  }

  loadDetails(): void {
    const record = this.maintenanceRecord;

    if (!record) {
      return;
    }

    this.loading.set(true);

    forkJoin({
      people: this.maintenanceDetailService.findPeople(record.id),
      materials: this.maintenanceDetailService.findMaterials(record.id)
    }).subscribe({
      next: ({ people, materials }) => {
        this.people.set(people);
        this.materials.set(materials);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.loadError')));
      }
    });
  }

  addPerson(): void {
    const record = this.maintenanceRecord;

    if (!record) {
      return;
    }

    if (this.personForm.invalid) {
      this.personForm.markAllAsTouched();
      return;
    }

    this.addingPerson.set(true);

    this.maintenanceDetailService.addPerson(record.id, this.personForm.getRawValue()).subscribe({
      next: () => {
        this.addingPerson.set(false);
        this.toastService.success(this.languageService.instant('maintenance.details.messages.personAdded'));
        this.personForm.reset({ maintenancePersonId: '' });
        this.detailsChanged.emit();
        this.loadDetails();
      },
      error: (error: unknown) => {
        this.addingPerson.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.personAddError')));
      }
    });
  }

  editMaterial(material: MaintenanceMaterialUsed): void {
    this.editingMaterial.set(material);
    this.materialForm.reset({
      materialId: material.materialId ?? '',
      materialNameSnapshot: material.materialNameSnapshot ?? '',
      quantity: material.quantity !== null && material.quantity !== undefined ? String(material.quantity) : '',
      unit: material.unit ?? '',
      notes: material.notes ?? ''
    });
  }

  cancelEditMaterial(): void {
    if (this.savingMaterial()) {
      return;
    }

    this.editingMaterial.set(null);
    this.materialForm.reset({
      materialId: '',
      materialNameSnapshot: '',
      quantity: '',
      unit: '',
      notes: ''
    });
  }

  saveMaterial(): void {
    const record = this.maintenanceRecord;

    if (!record) {
      return;
    }

    const rawValue = this.materialForm.getRawValue();

    if (!rawValue.materialId && !rawValue.materialNameSnapshot.trim()) {
      this.toastService.warning(this.languageService.instant('maintenance.details.messages.materialRequired'));
      return;
    }

    if (this.materialForm.invalid) {
      this.materialForm.markAllAsTouched();
      return;
    }

    const request = {
      materialId: rawValue.materialId || null,
      materialNameSnapshot: rawValue.materialNameSnapshot.trim() || null,
      quantity: rawValue.quantity === '' ? null : Number(rawValue.quantity),
      unit: rawValue.unit.trim() || null,
      notes: rawValue.notes.trim() || null
    };

    this.savingMaterial.set(true);

    const editingMaterial = this.editingMaterial();

    const saveRequest = editingMaterial
      ? this.maintenanceDetailService.updateMaterial(record.id, editingMaterial.id, request)
      : this.maintenanceDetailService.addMaterial(record.id, request);

    saveRequest.subscribe({
      next: () => {
        this.savingMaterial.set(false);
        this.toastService.success(
          editingMaterial
            ? this.languageService.instant('maintenance.details.messages.materialUpdated')
            : this.languageService.instant('maintenance.details.messages.materialAdded')
        );
        this.cancelEditMaterial();
        this.detailsChanged.emit();
        this.loadDetails();
      },
      error: (error: unknown) => {
        this.savingMaterial.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.materialSaveError')));
      }
    });
  }

  requestRemovePerson(person: MaintenanceRecordPerson): void {
    this.deleteTarget.set({
      type: 'person',
      id: person.id,
      name: person.fullName
    });
  }

  requestRemoveMaterial(material: MaintenanceMaterialUsed): void {
    this.deleteTarget.set({
      type: 'material',
      id: material.id,
      name: this.materialDisplayName(material)
    });
  }

  cancelDelete(): void {
    if (this.deleting()) {
      return;
    }

    this.deleteTarget.set(null);
  }

  confirmDelete(): void {
    const record = this.maintenanceRecord;
    const target = this.deleteTarget();

    if (!record || !target) {
      return;
    }

    this.deleting.set(true);

    const deleteRequest = target.type === 'person'
      ? this.maintenanceDetailService.removePerson(record.id, target.id)
      : this.maintenanceDetailService.removeMaterial(record.id, target.id);

    deleteRequest.subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toastService.success(
          target.type === 'person'
            ? this.languageService.instant('maintenance.details.messages.personRemoved')
            : this.languageService.instant('maintenance.details.messages.materialRemoved')
        );
        this.detailsChanged.emit();
        this.loadDetails();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.deleteError')));
      }
    });
  }

  onMaterialSelected(materialId: string): void {
    const material = this.materialOptions.find((item) => item.id === materialId);

    if (material?.unit && !this.materialForm.controls.unit.value) {
      this.materialForm.controls.unit.setValue(material.unit);
    }
  }

  materialDisplayName(material: MaintenanceMaterialUsed): string {
    return material.materialName ?? material.materialNameSnapshot ?? '—';
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.people.set([]);
    this.materials.set([]);
    this.deleteTarget.set(null);
    this.editingMaterial.set(null);
    this.personForm.reset({ maintenancePersonId: '' });
    this.materialForm.reset({
      materialId: '',
      materialNameSnapshot: '',
      quantity: '',
      unit: '',
      notes: ''
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
