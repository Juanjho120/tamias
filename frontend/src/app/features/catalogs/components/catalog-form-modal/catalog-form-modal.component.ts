import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  CatalogConfig,
  CatalogFieldConfig,
  CatalogItem,
  CatalogRequest,
  CatalogStatus,
  CATALOG_STATUSES
} from '../../models/catalog.model';

type CatalogFormGroup = FormGroup<Record<string, FormControl<string | boolean>>>;

@Component({
  selector: 'app-catalog-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './catalog-form-modal.component.html'
})
export class CatalogFormModalComponent implements OnChanges {
  @Input() open = false;
  @Input() config: CatalogConfig | null = null;
  @Input() item: CatalogItem | null = null;
  @Input() loading = false;

  @Output() save = new EventEmitter<CatalogRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = CATALOG_STATUSES.filter((status) => status !== 'DELETED');

  form: CatalogFormGroup = this.createEmptyForm();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['config']) {
      this.form = this.buildForm();
    }

    if (changes['item'] || changes['open'] || changes['config']) {
      this.patchForm();
    }
  }

  submit(): void {
    if (!this.config) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    const request: CatalogRequest = {};

    for (const field of this.config.fields) {
      const value = rawValue[field.key];

      if (field.type === 'checkbox') {
        request[field.key] = Boolean(value);
      } else {
        request[field.key] = typeof value === 'string' ? value.trim() || null : null;
      }
    }

    request['status'] = rawValue['status'] as CatalogStatus;
    this.save.emit(request);
  }

  isInvalid(field: CatalogFieldConfig | 'status'): boolean {
    const key = typeof field === 'string' ? field : field.key;
    const control = this.form.get(key);

    if (!control) {
      return false;
    }

    return control.invalid && (control.touched || control.dirty);
  }

  fieldInputType(field: CatalogFieldConfig): string {
    switch (field.type) {
      case 'email':
        return 'email';
      case 'url':
        return 'url';
      default:
        return 'text';
    }
  }

  private createEmptyForm(): CatalogFormGroup {
    return new FormGroup<Record<string, FormControl<string | boolean>>>({
      status: new FormControl<string | boolean>('ACTIVE', { nonNullable: true, validators: [Validators.required] })
    });
  }

  private buildForm(): CatalogFormGroup {
    const controls: Record<string, FormControl<string | boolean>> = {
      status: new FormControl<string | boolean>('ACTIVE', { nonNullable: true, validators: [Validators.required] })
    };

    if (this.config) {
      for (const field of this.config.fields) {
        const validators = [];

        if (field.required) {
          validators.push(Validators.required);
        }

        if (field.maxLength) {
          validators.push(Validators.maxLength(field.maxLength));
        }

        if (field.type === 'email') {
          validators.push(Validators.email);
        }

        controls[field.key] = new FormControl<string | boolean>(
          field.type === 'checkbox' ? false : '',
          { nonNullable: true, validators }
        );
      }
    }

    return new FormGroup<Record<string, FormControl<string | boolean>>>(controls);
  }

  private patchForm(): void {
    if (!this.config) {
      return;
    }

    const value: Record<string, string | boolean> = {
      status: this.item?.status === 'DELETED' ? 'INACTIVE' : this.item?.status ?? 'ACTIVE'
    };

    for (const field of this.config.fields) {
      const rawValue = this.item?.[field.key as keyof CatalogItem];

      if (field.type === 'checkbox') {
        value[field.key] = Boolean(rawValue);
      } else {
        value[field.key] = typeof rawValue === 'string' ? rawValue : '';
      }
    }

    this.form.reset(value);
  }
}
