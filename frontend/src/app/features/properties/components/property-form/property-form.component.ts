import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { Property, PropertyRequest, PropertyStatus, PROPERTY_STATUSES } from '../../models/property.model';

@Component({
  selector: 'app-property-form',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './property-form.component.html'
})
export class PropertyFormComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() property: Property | null = null;
  @Input() loading = false;

  @Output() save = new EventEmitter<PropertyRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = PROPERTY_STATUSES.filter((status) => status !== 'DELETED');

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    address: [''],
    description: [''],
    status: this.formBuilder.nonNullable.control<PropertyStatus>('ACTIVE', [Validators.required])
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['property']) {
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
      name: rawValue.name.trim(),
      address: rawValue.address?.trim() || null,
      description: rawValue.description?.trim() || null,
      status: rawValue.status
    });
  }

  isInvalid(controlName: 'name' | 'status'): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.property) {
      this.form.reset({
        name: '',
        address: '',
        description: '',
        status: 'ACTIVE'
      });
      return;
    }

    this.form.reset({
      name: this.property.name,
      address: this.property.address ?? '',
      description: this.property.description ?? '',
      status: this.property.status === 'DELETED' ? 'INACTIVE' : this.property.status
    });
  }
}
