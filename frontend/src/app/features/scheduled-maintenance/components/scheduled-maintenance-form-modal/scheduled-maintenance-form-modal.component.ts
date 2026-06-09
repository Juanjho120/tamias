import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  ScheduledMaintenance,
  ScheduledMaintenanceFrequency,
  ScheduledMaintenanceRequest,
  ScheduledMaintenanceStatus,
  SCHEDULED_MAINTENANCE_FREQUENCIES,
  SCHEDULED_MAINTENANCE_STATUSES
} from '../../models/scheduled-maintenance.model';
import {
  ScheduledMaintenancePersonOption,
  ScheduledMaintenancePropertyOption,
  ScheduledMaintenanceReferenceOption
} from '../../models/scheduled-maintenance-reference.model';

@Component({
  selector: 'app-scheduled-maintenance-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './scheduled-maintenance-form-modal.component.html'
})
export class ScheduledMaintenanceFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() scheduledMaintenance: ScheduledMaintenance | null = null;
  @Input() properties: ScheduledMaintenancePropertyOption[] = [];
  @Input() categories: ScheduledMaintenanceReferenceOption[] = [];
  @Input() types: ScheduledMaintenanceReferenceOption[] = [];
  @Input() people: ScheduledMaintenancePersonOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<ScheduledMaintenanceRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = SCHEDULED_MAINTENANCE_STATUSES.filter((status) => status !== 'DELETED');
  readonly frequencies = SCHEDULED_MAINTENANCE_FREQUENCIES;

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: ['', [Validators.required]],
    maintenanceCategoryId: [''],
    maintenanceTypeId: [''],
    maintenancePersonId: [''],
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    frequency: this.formBuilder.nonNullable.control<ScheduledMaintenanceFrequency>('MONTHLY', [Validators.required]),
    intervalValue: [1, [Validators.required, Validators.min(1)]],
    startDate: ['', [Validators.required]],
    endDate: [''],
    nextDueDate: [''],
    estimatedCost: [''],
    status: this.formBuilder.nonNullable.control<ScheduledMaintenanceStatus>('ACTIVE', [Validators.required])
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['scheduledMaintenance'] || changes['open']) {
      this.patchForm();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.save.emit({
      propertyId: rawValue.propertyId,
      maintenanceCategoryId: rawValue.maintenanceCategoryId || null,
      maintenanceTypeId: rawValue.maintenanceTypeId || null,
      maintenancePersonId: rawValue.maintenancePersonId || null,
      title: rawValue.title.trim(),
      description: rawValue.description.trim() || null,
      frequency: rawValue.frequency,
      intervalValue: Number(rawValue.intervalValue),
      startDate: rawValue.startDate,
      endDate: rawValue.endDate || null,
      nextDueDate: rawValue.nextDueDate || null,
      estimatedCost: rawValue.estimatedCost === '' ? null : Number(rawValue.estimatedCost),
      status: rawValue.status
    });
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.scheduledMaintenance) {
      this.form.reset({
        propertyId: '',
        maintenanceCategoryId: '',
        maintenanceTypeId: '',
        maintenancePersonId: '',
        title: '',
        description: '',
        frequency: 'MONTHLY',
        intervalValue: 1,
        startDate: this.today(),
        endDate: '',
        nextDueDate: '',
        estimatedCost: '',
        status: 'ACTIVE'
      });
      return;
    }

    this.form.reset({
      propertyId: this.scheduledMaintenance.propertyId,
      maintenanceCategoryId: this.scheduledMaintenance.maintenanceCategoryId ?? '',
      maintenanceTypeId: this.scheduledMaintenance.maintenanceTypeId ?? '',
      maintenancePersonId: this.scheduledMaintenance.maintenancePersonId ?? '',
      title: this.scheduledMaintenance.title,
      description: this.scheduledMaintenance.description ?? '',
      frequency: this.scheduledMaintenance.frequency,
      intervalValue: this.scheduledMaintenance.intervalValue,
      startDate: this.scheduledMaintenance.startDate,
      endDate: this.scheduledMaintenance.endDate ?? '',
      nextDueDate: this.scheduledMaintenance.nextDueDate ?? '',
      estimatedCost: this.scheduledMaintenance.estimatedCost !== null && this.scheduledMaintenance.estimatedCost !== undefined
        ? String(this.scheduledMaintenance.estimatedCost)
        : '',
      status: this.scheduledMaintenance.status === 'DELETED' ? 'COMPLETED' : this.scheduledMaintenance.status
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
