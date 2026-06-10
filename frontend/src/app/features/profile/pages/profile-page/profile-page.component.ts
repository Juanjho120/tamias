import { NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../../../core/models/api-error.model';
import { AuthService } from '../../../../core/services/auth.service';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ProfileService } from '../../services/profile.service';

const matchingPasswordsValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = control.get('newPassword')?.value;
  const confirmNewPassword = control.get('confirmNewPassword')?.value;

  if (!newPassword || !confirmNewPassword) {
    return null;
  }

  return newPassword === confirmNewPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './profile-page.component.html'
})
export class ProfilePageComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly profileService = inject(ProfileService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly passwordChangeRequired = this.authService.passwordChangeRequired;

  readonly loading = signal(false);
  readonly savingProfile = signal(false);
  readonly savingPassword = signal(false);

  readonly displayName = computed(() => {
    const user = this.user();
    return user ? `${user.firstName} ${user.lastName}` : '';
  });

  readonly profileForm = this.formBuilder.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]]
  });

  readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
    confirmNewPassword: ['', [Validators.required]]
  }, {
    validators: [matchingPasswordsValidator]
  });

  ngOnInit(): void {
    this.patchProfileFormFromSession();
    this.loadProfile();
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const rawValue = this.profileForm.getRawValue();

    this.savingProfile.set(true);

    this.profileService.updateProfile({
      firstName: rawValue.firstName.trim(),
      lastName: rawValue.lastName.trim()
    }).subscribe({
      next: (updatedUser) => {
        this.authService.updateCurrentUser(updatedUser);
        this.patchProfileFormFromSession();
        this.savingProfile.set(false);
        this.toastService.success(this.languageService.instant('profile.messages.profileUpdated'));
      },
      error: (error: unknown) => {
        this.savingProfile.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('profile.messages.profileUpdateError')));
      }
    });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const rawValue = this.passwordForm.getRawValue();
    const wasRequired = this.passwordChangeRequired();

    this.savingPassword.set(true);

    this.profileService.changePassword({
      currentPassword: rawValue.currentPassword,
      newPassword: rawValue.newPassword,
      confirmNewPassword: rawValue.confirmNewPassword
    }).subscribe({
      next: (updatedUser) => {
        this.authService.updateCurrentUser(updatedUser);
        this.passwordForm.reset({
          currentPassword: '',
          newPassword: '',
          confirmNewPassword: ''
        });
        this.savingPassword.set(false);
        this.toastService.success(this.languageService.instant('profile.messages.passwordUpdated'));

        if (wasRequired) {
          this.router.navigateByUrl('/dashboard');
        }
      },
      error: (error: unknown) => {
        this.savingPassword.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('profile.messages.passwordUpdateError')));
      }
    });
  }

  isProfileInvalid(controlName: keyof typeof this.profileForm.controls): boolean {
    const control = this.profileForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isPasswordInvalid(controlName: keyof typeof this.passwordForm.controls): boolean {
    const control = this.passwordForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  hasPasswordMismatch(): boolean {
    return this.passwordForm.hasError('passwordMismatch')
      && (this.passwordForm.controls.confirmNewPassword.touched || this.passwordForm.controls.confirmNewPassword.dirty);
  }

  private loadProfile(): void {
    this.loading.set(true);

    this.profileService.getCurrentProfile().subscribe({
      next: (profile) => {
        this.authService.updateCurrentUser(profile);
        this.patchProfileFormFromSession();
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private patchProfileFormFromSession(): void {
    const user = this.user();

    if (!user) {
      return;
    }

    this.profileForm.patchValue({
      firstName: user.firstName,
      lastName: user.lastName
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
