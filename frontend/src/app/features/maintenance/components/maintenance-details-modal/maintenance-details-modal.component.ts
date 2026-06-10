import { DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceRecordItem, MaintenanceRecordPerson } from '../../models/maintenance-detail.model';
import { MaintenanceInventoryItemOption, MaintenancePersonOption } from '../../models/maintenance-reference.model';
import { MaintenanceRecordSummary } from '../../models/maintenance-record.model';
import { MaintenanceDetailService } from '../../services/maintenance-detail.service';

type DeleteTargetType = 'person' | 'item';

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
  @Input() inventoryItemOptions: MaintenanceInventoryItemOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() detailsChanged = new EventEmitter<void>();

  readonly people = signal<MaintenanceRecordPerson[]>([]);
  readonly items = signal<MaintenanceRecordItem[]>([]);

  readonly loading = signal(false);
  readonly addingPerson = signal(false);
  readonly savingItem = signal(false);
  readonly deleting = signal(false);
  readonly deleteTarget = signal<DeleteTarget | null>(null);
  readonly editingItem = signal<MaintenanceRecordItem | null>(null);

  readonly personForm = this.formBuilder.nonNullable.group({
    maintenancePersonId: ['', [Validators.required]]
  });

  readonly itemForm = this.formBuilder.nonNullable.group({
    inventoryItemId: [''],
    itemNameSnapshot: [''],
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
      : 'maintenance.details.confirmRemoveItemMessage';

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
    if (this.loading() || this.addingPerson() || this.savingItem() || this.deleting()) {
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
      items: this.maintenanceDetailService.findItems(record.id)
    }).subscribe({
      next: ({ people, items }) => {
        this.people.set(people);
        this.items.set(items);
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

  editItem(item: MaintenanceRecordItem): void {
    this.editingItem.set(item);
    this.itemForm.reset({
      inventoryItemId: item.inventoryItemId ?? '',
      itemNameSnapshot: item.itemNameSnapshot ?? '',
      quantity: item.quantity !== null && item.quantity !== undefined ? String(item.quantity) : '',
      unit: item.unit ?? '',
      notes: item.notes ?? ''
    });
  }

  cancelEditItem(): void {
    if (this.savingItem()) {
      return;
    }

    this.editingItem.set(null);
    this.itemForm.reset({
      inventoryItemId: '',
      itemNameSnapshot: '',
      quantity: '',
      unit: '',
      notes: ''
    });
  }

  saveItem(): void {
    const record = this.maintenanceRecord;

    if (!record) {
      return;
    }

    const rawValue = this.itemForm.getRawValue();

    if (!rawValue.inventoryItemId && !rawValue.itemNameSnapshot.trim()) {
      this.toastService.warning(this.languageService.instant('maintenance.details.messages.materialRequired'));
      return;
    }

    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }

    const request = {
      inventoryItemId: rawValue.inventoryItemId || null,
      itemNameSnapshot: rawValue.itemNameSnapshot.trim() || null,
      quantity: rawValue.quantity === '' ? null : Number(rawValue.quantity),
      unit: rawValue.unit.trim() || null,
      notes: rawValue.notes.trim() || null
    };

    this.savingItem.set(true);

    const editingItem = this.editingItem();

    const saveRequest = editingItem
      ? this.maintenanceDetailService.updateItem(record.id, editingItem.id, request)
      : this.maintenanceDetailService.addItem(record.id, request);

    saveRequest.subscribe({
      next: () => {
        this.savingItem.set(false);
        this.toastService.success(
          editingItem
            ? this.languageService.instant('maintenance.details.messages.materialUpdated')
            : this.languageService.instant('maintenance.details.messages.materialAdded')
        );
        this.cancelEditItem();
        this.detailsChanged.emit();
        this.loadDetails();
      },
      error: (error: unknown) => {
        this.savingItem.set(false);
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

  requestRemoveItem(item: MaintenanceRecordItem): void {
    this.deleteTarget.set({
      type: 'item',
      id: item.id,
      name: this.itemDisplayName(item)
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
      : this.maintenanceDetailService.removeItem(record.id, target.id);

    deleteRequest.subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toastService.success(
          target.type === 'person'
            ? this.languageService.instant('maintenance.details.messages.personRemoved')
            : this.languageService.instant('maintenance.details.messages.itemRemoved')
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

  onInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItem = this.inventoryItemOptions.find((item) => item.id === inventoryItemId);

    if (inventoryItem?.unit && !this.itemForm.controls.unit.value) {
      this.itemForm.controls.unit.setValue(inventoryItem.unit);
    }
  }

  itemDisplayName(item: MaintenanceRecordItem): string {
    return item.inventoryItemName ?? item.itemNameSnapshot ?? '—';
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.people.set([]);
    this.items.set([]);
    this.deleteTarget.set(null);
    this.editingItem.set(null);
    this.personForm.reset({ maintenancePersonId: '' });
    this.itemForm.reset({
      inventoryItemId: '',
      itemNameSnapshot: '',
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
