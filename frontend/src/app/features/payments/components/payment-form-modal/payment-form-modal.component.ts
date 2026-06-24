import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { Payment, PaymentMethod, PaymentRequest, PAYMENT_METHODS } from '../../models/payment.model';
import { PaymentCategoryOption, PaymentPropertyOption } from '../../models/payment-reference.model';

@Component({
  selector: 'app-payment-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './payment-form-modal.component.html'
})
export class PaymentFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() payment: Payment | null = null;
  @Input() properties: PaymentPropertyOption[] = [];
  @Input() categories: PaymentCategoryOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<PaymentRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly methods = PAYMENT_METHODS;

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: [''],
    categoryId: ['', [Validators.required]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    description: [''],
    method: this.formBuilder.nonNullable.control<PaymentMethod>('CASH', [Validators.required]),
    amount: ['', [Validators.required, Validators.min(0)]],
    responsible: ['', [Validators.maxLength(150)]],
    payDate: ['', [Validators.required]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['payment'] || changes['open']) {
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
      propertyId: rawValue.propertyId || null,
      categoryId: rawValue.categoryId,
      name: rawValue.name.trim(),
      description: rawValue.description.trim() || null,
      method: rawValue.method,
      amount: Number(rawValue.amount),
      responsible: rawValue.responsible.trim() || null,
      payDate: rawValue.payDate
    });
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.payment) {
      this.form.reset({
        propertyId: '',
        categoryId: '',
        name: '',
        description: '',
        method: 'CASH',
        amount: '',
        responsible: '',
        payDate: this.today()
      });
      return;
    }

    this.form.reset({
      propertyId: this.payment.propertyId ?? '',
      categoryId: this.payment.categoryId,
      name: this.payment.name,
      description: this.payment.description ?? '',
      method: this.payment.method,
      amount: this.payment.amount !== null && this.payment.amount !== undefined ? String(this.payment.amount) : '',
      responsible: this.payment.responsible ?? '',
      payDate: this.payment.payDate
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
