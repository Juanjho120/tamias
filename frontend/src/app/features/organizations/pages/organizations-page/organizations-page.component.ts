import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Organization, OrganizationStatus } from '../../models/organization.model';
import {
  OrganizationFormModalComponent,
  OrganizationFormMode,
  OrganizationFormSubmit
} from '../../components/organization-form-modal/organization-form-modal.component';
import { OrganizationService } from '../../services/organization.service';

@Component({
  selector: 'app-organizations-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    NgClass,
    ConfirmModalComponent,
    OrganizationFormModalComponent
  ],
  templateUrl: './organizations-page.component.html',
  styles: [
    `
      .organization-logo-preview,
      .organization-logo-fallback {
        width: 42px;
        height: 42px;
        min-width: 42px;
        border-radius: 0.75rem;
      }

      .organization-logo-preview {
        object-fit: cover;
        border: 1px solid rgba(0, 0, 0, 0.08);
        background: #fff;
      }

      .organization-logo-fallback {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border: 1px solid rgba(13, 110, 253, 0.18);
        background: rgba(13, 110, 253, 0.08);
        color: #0d6efd;
        font-size: 0.82rem;
        font-weight: 700;
        letter-spacing: 0.02em;
        text-transform: uppercase;
      }
    `
  ]
})
export class OrganizationsPageComponent implements OnInit {
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);

  readonly currentUser = this.authService.currentUser;
  readonly isSuperAdmin = computed(() => this.currentUser()?.role === 'SUPER_ADMIN');
  readonly isAdministrator = computed(() => this.currentUser()?.role === 'ADMINISTRATOR');
  readonly canCreateOrganizations = computed(() => this.isSuperAdmin());

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly uploadingLogoId = signal<string | null>(null);
  readonly deletingLogoId = signal<string | null>(null);
  readonly updatingStatusId = signal<string | null>(null);
  readonly organizations = signal<Organization[]>([]);
  readonly selectedOrganization = signal<Organization | null>(null);
  readonly formVisible = signal(false);
  readonly formMode = signal<OrganizationFormMode>('edit');
  readonly logoToDelete = signal<Organization | null>(null);
  readonly statusToUpdate = signal<{ organization: Organization; status: OrganizationStatus } | null>(null);

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return 'Sin organizaciones';
    }
    return `Página ${this.page() + 1} de ${this.totalPages()}`;
  });

  readonly statusConfirmMessage = computed(() => {
    const target = this.statusToUpdate();
    if (!target) {
      return '';
    }
    const action = target.status === 'ACTIVE' ? 'activar' : 'desactivar';
    return `¿Seguro que deseas ${action} la organización "${target.organization.name}"?`;
  });

  readonly logoDeleteMessage = computed(() => {
    const organization = this.logoToDelete();
    if (!organization) {
      return '';
    }
    return `¿Eliminar el logo de la organización "${organization.name}"?`;
  });

  constructor(private readonly organizationService: OrganizationService) { }

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.loading.set(true);
    this.organizationService.findAll({
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<Organization>) => {
        this.organizations.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, 'No se pudieron cargar las organizaciones.'));
      }
    });
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }
    this.page.update((value) => value - 1);
    this.loadOrganizations();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }
    this.page.update((value) => value + 1);
    this.loadOrganizations();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadOrganizations();
  }

  openCreateForm(): void {
    if (!this.canCreateOrganizations()) {
      return;
    }
    this.formMode.set('create');
    this.selectedOrganization.set(null);
    this.formVisible.set(true);
  }

  openEditForm(organization: Organization): void {
    this.formMode.set('edit');
    this.selectedOrganization.set(organization);
    this.formVisible.set(true);
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }
    this.formVisible.set(false);
    this.selectedOrganization.set(null);
    this.formMode.set('edit');
  }

  saveOrganization(request: OrganizationFormSubmit): void {
    this.saving.set(true);
    const selectedOrganization = this.selectedOrganization();
    const saveRequest = this.formMode() === 'edit' && selectedOrganization
      ? this.organizationService.update(selectedOrganization.id, request)
      : this.organizationService.create(request);

    saveRequest.subscribe({
      next: (organization: Organization) => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? 'Organización actualizada correctamente.'
            : 'Organización creada correctamente.'
        );
        this.syncCurrentOrganization(organization);
        this.closeForm();
        this.loadOrganizations();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo guardar la organización.'));
      }
    });
  }

  requestActivate(organization: Organization): void {
    this.statusToUpdate.set({ organization, status: 'ACTIVE' });
  }

  requestDeactivate(organization: Organization): void {
    this.statusToUpdate.set({ organization, status: 'INACTIVE' });
  }

  cancelStatusUpdate(): void {
    if (this.updatingStatusId()) {
      return;
    }
    this.statusToUpdate.set(null);
  }

  confirmStatusUpdate(): void {
    const target = this.statusToUpdate();
    if (!target) {
      return;
    }

    this.updatingStatusId.set(target.organization.id);
    this.organizationService.updateStatus(target.organization.id, target.status).subscribe({
      next: () => {
        this.updatingStatusId.set(null);
        this.statusToUpdate.set(null);
        this.toastService.success(
          target.status === 'ACTIVE'
            ? 'Organización activada correctamente.'
            : 'Organización desactivada correctamente.'
        );
        this.loadOrganizations();
      },
      error: (error: unknown) => {
        this.updatingStatusId.set(null);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo actualizar el estado de la organización.'));
      }
    });
  }

  uploadLogo(organization: Organization, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) {
      return;
    }

    this.uploadingLogoId.set(organization.id);
    this.organizationService.uploadLogo(organization.id, file).subscribe({
      next: (updatedOrganization: Organization) => {
        this.uploadingLogoId.set(null);
        this.toastService.success('Logo actualizado correctamente.');
        this.syncCurrentOrganization(updatedOrganization);
        this.loadOrganizations();
      },
      error: (error: unknown) => {
        this.uploadingLogoId.set(null);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo subir el logo.'));
      }
    });
  }

  requestDeleteLogo(organization: Organization): void {
    this.logoToDelete.set(organization);
  }

  cancelDeleteLogo(): void {
    if (this.deletingLogoId()) {
      return;
    }
    this.logoToDelete.set(null);
  }

  confirmDeleteLogo(): void {
    const organization = this.logoToDelete();
    if (!organization) {
      return;
    }

    this.deletingLogoId.set(organization.id);
    this.organizationService.deleteLogo(organization.id).subscribe({
      next: (updatedOrganization: Organization) => {
        this.deletingLogoId.set(null);
        this.logoToDelete.set(null);
        this.toastService.success('Logo eliminado correctamente.');
        this.syncCurrentOrganization(updatedOrganization);
        this.loadOrganizations();
      },
      error: (error: unknown) => {
        this.deletingLogoId.set(null);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo eliminar el logo.'));
      }
    });
  }

  organizationInitials(organization: Organization): string {
    const initials = organization.name
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word.charAt(0).toUpperCase())
      .join('');
    return initials || 'ORG';
  }

  statusBadgeClass(status: OrganizationStatus): string {
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

  canEditOrganization(organization: Organization): boolean {
    return this.isSuperAdmin() || this.currentUser()?.organization.id === organization.id;
  }

  canChangeStatus(organization: Organization): boolean {
    return this.isSuperAdmin() && this.currentUser()?.organization.id !== organization.id;
  }

  private syncCurrentOrganization(organization: Organization): void {
    const currentUser = this.currentUser();
    if (!currentUser || currentUser.organization.id !== organization.id) {
      return;
    }

    this.authService.updateCurrentUser({
      ...currentUser,
      organization: {
        ...currentUser.organization,
        name: organization.name,
        logoUrl: organization.logoUrl
      }
    });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
