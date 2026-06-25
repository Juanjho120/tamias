import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceRecordPerson } from '../../models/maintenance-detail.model';
import { MaintenancePersonOption } from '../../models/maintenance-reference.model';
import { MaintenanceRecordSummary } from '../../models/maintenance-record.model';
import { MaintenanceDetailService } from '../../services/maintenance-detail.service';

interface PersonDeleteTarget {
  id: string;
  name: string;
}

@Component({
  selector: 'app-maintenance-people-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, ConfirmModalComponent],
  templateUrl: './maintenance-people-modal.component.html'
})
export class MaintenancePeopleModalComponent implements OnChanges {
  private readonly maintenanceDetailService = inject(MaintenanceDetailService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() maintenanceRecord: MaintenanceRecordSummary | null = null;
  @Input() peopleOptions: MaintenancePersonOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() peopleChanged = new EventEmitter<void>();

  readonly people = signal<MaintenanceRecordPerson[]>([]);
  readonly loading = signal(false);
  readonly addingPerson = signal(false);
  readonly deleting = signal(false);
  readonly deleteTarget = signal<PersonDeleteTarget | null>(null);

  readonly personForm = this.formBuilder.nonNullable.group({
    maintenancePersonId: ['', [Validators.required]]
  });

  readonly deleteMessage = computed(() => {
    const target = this.deleteTarget();

    if (!target) {
      return '';
    }

    return this.languageService.instant('maintenance.details.confirmRemovePersonMessage', { name: target.name });
  });

  readonly availablePeople = computed(() => {
    const assignedIds = new Set(this.people().map((person) => person.maintenancePersonId));
    return this.peopleOptions.filter((person) => !assignedIds.has(person.id));
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['maintenanceRecord']) && this.open && this.maintenanceRecord) {
      this.loadPeople();
    }
  }

  requestClose(): void {
    if (this.loading() || this.addingPerson() || this.deleting()) {
      return;
    }

    this.resetState();
    this.close.emit();
  }

  loadPeople(): void {
    const record = this.maintenanceRecord;

    if (!record) {
      return;
    }

    this.loading.set(true);

    this.maintenanceDetailService.findPeople(record.id).subscribe({
      next: (people) => {
        this.people.set(people);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.loadError'))
        );
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
        this.peopleChanged.emit();
        this.loadPeople();
      },
      error: (error: unknown) => {
        this.addingPerson.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.personAddError'))
        );
      }
    });
  }

  requestRemovePerson(person: MaintenanceRecordPerson): void {
    this.deleteTarget.set({ id: person.id, name: person.fullName });
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

    this.maintenanceDetailService.removePerson(record.id, target.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toastService.success(this.languageService.instant('maintenance.details.messages.personRemoved'));
        this.peopleChanged.emit();
        this.loadPeople();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.deleteError'))
        );
      }
    });
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.people.set([]);
    this.deleteTarget.set(null);
    this.personForm.reset({ maintenancePersonId: '' });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
