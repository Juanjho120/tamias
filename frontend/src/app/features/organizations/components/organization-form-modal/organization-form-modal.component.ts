import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  Organization,
  OrganizationCreateRequest,
  OrganizationUpdateRequest
} from '../../models/organization.model';

export type OrganizationFormMode = 'create' | 'edit';
export type OrganizationFormSubmit = OrganizationCreateRequest | OrganizationUpdateRequest;

@Component({
  selector: 'app-organization-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule],
  templateUrl: './organization-form-modal.component.html'
})
export class OrganizationFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() mode: OrganizationFormMode = 'create';
  @Input() organization: Organization | null = null;
  @Input() loading = false;
  @Input() canCreate = false;

  @Output() save = new EventEmitter<OrganizationFormSubmit>();
  @Output() cancel = new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] || changes['organization'] || changes['mode']) {
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
      description: rawValue.description?.trim() || null
    });
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.organization || this.mode === 'create') {
      this.form.reset({ name: '', description: '' });
      return;
    }

    this.form.reset({
      name: this.organization.name,
      description: this.organization.description ?? ''
    });
  }
}
