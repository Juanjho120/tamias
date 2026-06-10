import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  ROLE_CODES,
  RoleCode,
  User,
  UserCreateRequest,
  UserStatus,
  USER_STATUSES,
  UserUpdateRequest
} from '../../models/user.model';

export type UserFormMode = 'create' | 'edit';

export type UserFormSubmit = UserCreateRequest | UserUpdateRequest;

@Component({
  selector: 'app-user-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './user-form-modal.component.html'
})
export class UserFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() mode: UserFormMode = 'create';
  @Input() user: User | null = null;
  @Input() loading = false;

  @Output() save = new EventEmitter<UserFormSubmit>();
  @Output() cancel = new EventEmitter<void>();

  readonly roles = ROLE_CODES;
  readonly statuses = USER_STATUSES;

  readonly form = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    password: ['', [Validators.minLength(8), Validators.maxLength(100)]],
    role: this.formBuilder.nonNullable.control<RoleCode>('PROPERTY_MANAGER', [Validators.required]),
    status: this.formBuilder.nonNullable.control<UserStatus>('ACTIVE', [Validators.required])
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] || changes['user'] || changes['mode']) {
      this.patchForm();
    }
  }

  submit(): void {
    if (this.mode === 'create') {
      this.form.controls.password.addValidators(Validators.required);
    } else {
      this.form.controls.password.clearValidators();
    }

    this.form.controls.password.updateValueAndValidity();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    if (this.mode === 'create') {
      this.save.emit({
        firstName: rawValue.firstName.trim(),
        lastName: rawValue.lastName.trim(),
        email: rawValue.email.trim(),
        password: rawValue.password,
        role: rawValue.role
      });
      return;
    }

    this.save.emit({
      firstName: rawValue.firstName.trim(),
      lastName: rawValue.lastName.trim(),
      email: rawValue.email.trim(),
      role: rawValue.role,
      status: rawValue.status
    });
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.user || this.mode === 'create') {
      this.form.reset({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'PROPERTY_MANAGER',
        status: 'ACTIVE'
      });
      return;
    }

    this.form.reset({
      firstName: this.user.firstName,
      lastName: this.user.lastName,
      email: this.user.email,
      password: '',
      role: this.user.role,
      status: this.user.status === 'DELETED' ? 'INACTIVE' : this.user.status
    });
  }
}
