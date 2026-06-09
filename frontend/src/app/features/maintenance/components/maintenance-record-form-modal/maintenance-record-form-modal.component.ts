import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  MaintenanceRecord,
  MaintenanceRecordRequest,
  MaintenanceStatus,
  MAINTENANCE_STATUSES
} from '../../models/maintenance-record.model';
import { MaintenancePersonOption, MaintenanceReferenceOption, PropertyOption } from '../../models/maintenance-reference.model';

@Component({
  selector: 'app-maintenance-record-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './maintenance-record-form-modal.component.html'
})
export class MaintenanceRecordFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() record: MaintenanceRecord | null = null;
  @Input() properties: PropertyOption[] = [];
  @Input() categories: MaintenanceReferenceOption[] = [];
  @Input() types: MaintenanceReferenceOption[] = [];
  @Input() people: MaintenancePersonOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<MaintenanceRecordRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = MAINTENANCE_STATUSES.filter((status) => status !== 'DELETED');

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: ['', [Validators.required]],
    maintenanceCategoryId: [''],
    maintenanceTypeId: [''],
    maintenancePersonId: [''],
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    scheduledAt: [''],
    performedAt: [''],
    cost: [''],
    status: this.formBuilder.nonNullable.control<MaintenanceStatus>('PENDING', [Validators.required])
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['record'] || changes['open']) {
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
      scheduledAt: this.toOffsetDateTime(rawValue.scheduledAt),
      performedAt: this.toOffsetDateTime(rawValue.performedAt),
      cost: rawValue.cost === '' ? null : Number(rawValue.cost),
      status: rawValue.status
    });
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.record) {
      this.form.reset({
        propertyId: '',
        maintenanceCategoryId: '',
        maintenanceTypeId: '',
        maintenancePersonId: '',
        title: '',
        description: '',
        scheduledAt: '',
        performedAt: '',
        cost: '',
        status: 'PENDING'
      });
      return;
    }

    this.form.reset({
      propertyId: this.record.propertyId,
      maintenanceCategoryId: this.record.maintenanceCategoryId ?? '',
      maintenanceTypeId: this.record.maintenanceTypeId ?? '',
      maintenancePersonId: this.record.maintenancePersonId ?? '',
      title: this.record.title,
      description: this.record.description ?? '',
      scheduledAt: this.toDateTimeLocal(this.record.scheduledAt),
      performedAt: this.toDateTimeLocal(this.record.performedAt),
      cost: this.record.cost !== null && this.record.cost !== undefined ? String(this.record.cost) : '',
      status: this.record.status === 'DELETED' ? 'CANCELLED' : this.record.status
    });
  }

  private toDateTimeLocal(value: string | null): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const offset = date.getTimezoneOffset();
    const localDate = new Date(date.getTime() - offset * 60_000);

    return localDate.toISOString().slice(0, 16);
  }

  private toOffsetDateTime(value: string): string | null {
    if (!value) {
      return null;
    }

    return new Date(value).toISOString();
  }
}
