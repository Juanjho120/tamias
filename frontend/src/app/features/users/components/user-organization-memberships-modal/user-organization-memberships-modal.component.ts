import { DatePipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Organization } from '../../../organizations/models/organization.model';
import { OrganizationService } from '../../../organizations/services/organization.service';
import {
  ROLE_CODES_WITH_SUPER_ADMIN,
  RoleCode,
  USER_ORGANIZATION_MEMBERSHIP_STATUSES,
  UserOrganizationMembership,
  UserOrganizationMembershipStatus,
  UserSummary
} from '../../models/user.model';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-organization-memberships-modal',
  standalone: true,
  imports: [DatePipe, FormsModule, NgClass, TranslatePipe, ConfirmModalComponent],
  templateUrl: './user-organization-memberships-modal.component.html',
  styles: [
    `
      .organization-logo-preview {
        height: 32px;
        width: 32px;
        object-fit: cover;
      }

      .organization-logo-fallback {
        height: 32px;
        width: 32px;
      }
    `
  ]
})
export class UserOrganizationMembershipsModalComponent implements OnChanges {
  private readonly userService = inject(UserService);
  private readonly organizationService = inject(OrganizationService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  @Input() open = false;
  @Input() user: UserSummary | null = null;
  @Input() loading = false;

  @Output() close = new EventEmitter<void>();
  @Output() changed = new EventEmitter<void>();

  readonly roles = ROLE_CODES_WITH_SUPER_ADMIN;
  readonly statuses = USER_ORGANIZATION_MEMBERSHIP_STATUSES;

  readonly memberships = signal<UserOrganizationMembership[]>([]);
  readonly organizations = signal<Organization[]>([]);
  readonly loadingData = signal(false);
  readonly saving = signal(false);
  readonly deletingOrganizationId = signal<string | null>(null);
  readonly membershipToDelete = signal<UserOrganizationMembership | null>(null);

  selectedOrganizationId = '';
  selectedRole: RoleCode = 'READ_ONLY';

  readonly availableOrganizations = computed(() => {
    const assignedOrganizationIds = new Set(this.memberships().map((membership) => membership.organizationId));
    return this.organizations().filter((organization) => !assignedOrganizationIds.has(organization.id));
  });

  readonly deleteMessage = computed(() => {
    const membership = this.membershipToDelete();
    if (!membership) {
      return '';
    }

    return this.languageService.instant('userOrganizations.confirmDelete.message', {
      organization: membership.organizationName
    });
  });

  private readonly roleDrafts = signal<Record<string, RoleCode>>({});
  private readonly statusDrafts = signal<Record<string, UserOrganizationMembershipStatus>>({});

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] || changes['user']) {
      if (this.open && this.user) {
        this.loadData();
        return;
      }

      this.resetState();
    }
  }

  addMembership(): void {
    const user = this.user;
    if (!user || !this.selectedOrganizationId) {
      return;
    }

    this.saving.set(true);
    this.userService.createOrganizationMembership(user.id, {
      organizationId: this.selectedOrganizationId,
      role: this.selectedRole
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(this.languageService.instant('userOrganizations.messages.assigned'));
        this.selectedOrganizationId = '';
        this.selectedRole = 'READ_ONLY';
        this.loadData();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('userOrganizations.messages.assignError')));
      }
    });
  }

  saveMembership(membership: UserOrganizationMembership): void {
    const user = this.user;
    if (!user) {
      return;
    }

    this.saving.set(true);
    this.userService.updateOrganizationMembership(user.id, membership.organizationId, {
      role: this.roleDraft(membership),
      status: this.statusDraft(membership)
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(this.languageService.instant('userOrganizations.messages.updated'));
        this.loadData();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('userOrganizations.messages.updateError')));
      }
    });
  }

  requestDelete(membership: UserOrganizationMembership): void {
    this.membershipToDelete.set(membership);
  }

  cancelDelete(): void {
    if (this.deletingOrganizationId()) {
      return;
    }

    this.membershipToDelete.set(null);
  }

  confirmDelete(): void {
    const user = this.user;
    const membership = this.membershipToDelete();
    if (!user || !membership) {
      return;
    }

    this.deletingOrganizationId.set(membership.organizationId);
    this.userService.deleteOrganizationMembership(user.id, membership.organizationId).subscribe({
      next: () => {
        this.deletingOrganizationId.set(null);
        this.membershipToDelete.set(null);
        this.toastService.success(this.languageService.instant('userOrganizations.messages.deleted'));
        this.loadData();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.deletingOrganizationId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('userOrganizations.messages.deleteError')));
      }
    });
  }

  roleDraft(membership: UserOrganizationMembership): RoleCode {
    return this.roleDrafts()[membership.organizationId] ?? membership.role;
  }

  statusDraft(membership: UserOrganizationMembership): UserOrganizationMembershipStatus {
    return this.statusDrafts()[membership.organizationId] ?? membership.status;
  }

  updateSelectedRole(role: string): void {
    this.selectedRole = this.toRoleCode(role);
  }

  updateRoleDraft(membership: UserOrganizationMembership, role: string): void {
    this.roleDrafts.update((drafts) => ({ ...drafts, [membership.organizationId]: this.toRoleCode(role) }));
  }

  updateStatusDraft(membership: UserOrganizationMembership, status: string): void {
    this.statusDrafts.update((drafts) => ({ ...drafts, [membership.organizationId]: this.toMembershipStatus(status) }));
  }

  hasMembershipChanges(membership: UserOrganizationMembership): boolean {
    return this.roleDraft(membership) !== membership.role || this.statusDraft(membership) !== membership.status;
  }

  closeModal(): void {
    if (this.saving() || this.loadingData()) {
      return;
    }

    this.close.emit();
  }

  fullName(user: Pick<UserSummary, 'firstName' | 'lastName'>): string {
    return `${user.firstName} ${user.lastName}`.trim();
  }

  initials(name: string): string {
    const words = name.trim().split(/\s+/).filter(Boolean);
    return words.slice(0, 2).map((word) => word[0]?.toUpperCase()).join('') || '—';
  }

  roleBadgeClass(role: RoleCode): string {
    switch (role) {
      case 'SUPER_ADMIN':
        return 'text-bg-dark';
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

  statusBadgeClass(status: UserOrganizationMembershipStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'INACTIVE':
        return 'text-bg-secondary';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  private toRoleCode(value: string): RoleCode {
    return this.roles.includes(value as RoleCode) ? value as RoleCode : 'READ_ONLY';
  }

  private toMembershipStatus(value: string): UserOrganizationMembershipStatus {
    return this.statuses.includes(value as UserOrganizationMembershipStatus)
      ? value as UserOrganizationMembershipStatus
      : 'INACTIVE';
  }

  private loadData(): void {
    const user = this.user;
    if (!user) {
      return;
    }

    this.loadingData.set(true);
    forkJoin({
      memberships: this.userService.findOrganizationMemberships(user.id),
      organizations: this.organizationService.findAll({ page: 0, size: 500, sort: 'name,asc' })
    }).subscribe({
      next: ({ memberships, organizations }: {
        memberships: UserOrganizationMembership[];
        organizations: PageResponse<Organization>;
      }) => {
        this.memberships.set(memberships);
        this.organizations.set(organizations.content.filter((organization) => organization.status === 'ACTIVE'));
        this.roleDrafts.set(this.buildRoleDrafts(memberships));
        this.statusDrafts.set(this.buildStatusDrafts(memberships));
        this.loadingData.set(false);
      },
      error: (error: unknown) => {
        this.loadingData.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('userOrganizations.messages.loadError')));
      }
    });
  }

  private buildRoleDrafts(memberships: UserOrganizationMembership[]): Record<string, RoleCode> {
    return memberships.reduce<Record<string, RoleCode>>((drafts, membership) => {
      drafts[membership.organizationId] = membership.role;
      return drafts;
    }, {});
  }

  private buildStatusDrafts(
    memberships: UserOrganizationMembership[]
  ): Record<string, UserOrganizationMembershipStatus> {
    return memberships.reduce<Record<string, UserOrganizationMembershipStatus>>((drafts, membership) => {
      drafts[membership.organizationId] = membership.status;
      return drafts;
    }, {});
  }

  private resetState(): void {
    this.memberships.set([]);
    this.organizations.set([]);
    this.roleDrafts.set({});
    this.statusDrafts.set({});
    this.selectedOrganizationId = '';
    this.selectedRole = 'READ_ONLY';
    this.membershipToDelete.set(null);
    this.deletingOrganizationId.set(null);
    this.saving.set(false);
    this.loadingData.set(false);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
