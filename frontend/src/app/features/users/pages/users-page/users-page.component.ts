import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../../../core/services/auth.service';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { RoleCode, User, UserCreateRequest, UserStatus, UserSummary, UserUpdateRequest } from '../../models/user.model';
import { UserFormModalComponent, UserFormMode, UserFormSubmit } from '../../components/user-form-modal/user-form-modal.component';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    UserFormModalComponent
  ],
  templateUrl: './users-page.component.html'
})
export class UsersPageComponent implements OnInit {
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly updatingStatusId = signal<string | null>(null);

  readonly users = signal<UserSummary[]>([]);
  readonly selectedUser = signal<User | null>(null);
  readonly userToDelete = signal<UserSummary | null>(null);

  readonly formVisible = signal(false);
  readonly formMode = signal<UserFormMode>('create');

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('users.pagination.noUsers');
    }

    return this.languageService.instant('users.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const user = this.userToDelete();

    if (!user) {
      return '';
    }

    return this.languageService.instant('users.confirmDeleteMessage', {
      name: this.fullName(user)
    });
  });

  constructor(private readonly userService: UserService) {
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);

    this.userService.findAll({
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<UserSummary>) => {
        this.users.set(response.content);
        this.page.set(response.page);
        this.size.set(response.size);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.first.set(response.first);
        this.last.set(response.last);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('users.messages.loadError')));
      }
    });
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadUsers();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadUsers();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadUsers();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedUser.set(null);
    this.formVisible.set(true);
  }

  openEditForm(userId: string): void {
    this.loading.set(true);

    this.userService.findById(userId).subscribe({
      next: (user: User) => {
        this.selectedUser.set(user);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('users.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedUser.set(null);
    this.formMode.set('create');
  }

  saveUser(request: UserFormSubmit): void {
    this.saving.set(true);

    const selectedUser = this.selectedUser();

    const saveRequest = this.formMode() === 'edit' && selectedUser
      ? this.userService.update(selectedUser.id, request as UserUpdateRequest)
      : this.userService.create(request as UserCreateRequest);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('users.messages.updated')
            : this.languageService.instant('users.messages.created')
        );
        this.closeForm();
        this.loadUsers();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('users.messages.saveError')));
      }
    });
  }

  activateUser(user: UserSummary): void {
    this.updateStatus(user, 'ACTIVE');
  }

  deactivateUser(user: UserSummary): void {
    this.updateStatus(user, 'INACTIVE');
  }

  requestDelete(user: UserSummary): void {
    this.userToDelete.set(user);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.userToDelete.set(null);
  }

  confirmDelete(): void {
    const user = this.userToDelete();

    if (!user) {
      return;
    }

    this.deletingId.set(user.id);

    this.userService.delete(user.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.userToDelete.set(null);
        this.toastService.success(this.languageService.instant('users.messages.deleted'));
        this.loadUsers();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('users.messages.deleteError')));
      }
    });
  }

  fullName(user: Pick<UserSummary, 'firstName' | 'lastName'>): string {
    return `${user.firstName} ${user.lastName}`.trim();
  }

  statusBadgeClass(status: UserStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'INACTIVE':
        return 'text-bg-secondary';
      case 'INVITED':
        return 'text-bg-info';
      case 'LOCKED':
        return 'text-bg-warning';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  roleBadgeClass(role: RoleCode): string {
    switch (role) {
      case 'ADMINISTRATOR':
        return 'text-bg-primary';
      case 'PROPERTY_MANAGER':
        return 'text-bg-success';
      case 'MAINTENANCE_STAFF':
        return 'text-bg-warning';
      case 'READ_ONLY':
        return 'text-bg-secondary';
      default:
        return 'text-bg-secondary';
    }
  }

  isCurrentUser(user: UserSummary): boolean {
    return this.currentUser()?.id === user.id;
  }

  private updateStatus(user: UserSummary, status: UserStatus): void {
    this.updatingStatusId.set(user.id);

    this.userService.update(user.id, {
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      role: user.role,
      status
    }).subscribe({
      next: () => {
        this.updatingStatusId.set(null);
        this.toastService.success(
          status === 'ACTIVE'
            ? this.languageService.instant('users.messages.activated')
            : this.languageService.instant('users.messages.deactivated')
        );
        this.loadUsers();
      },
      error: (error: unknown) => {
        this.updatingStatusId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('users.messages.statusError')));
      }
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
