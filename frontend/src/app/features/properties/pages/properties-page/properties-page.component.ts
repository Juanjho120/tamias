import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { Property, PropertyRequest, PropertyStatus, PropertySummary, PROPERTY_STATUSES } from '../../models/property.model';
import { PropertyFormComponent } from '../../components/property-form/property-form.component';
import { PropertyService } from '../../services/property.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-properties-page',
  standalone: true,
  imports: [DatePipe, FormsModule, NgClass, TranslatePipe, PropertyFormComponent, ConfirmModalComponent],
  templateUrl: './properties-page.component.html'
})
export class PropertiesPageComponent implements OnInit {
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = PROPERTY_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);

  readonly properties = signal<PropertySummary[]>([]);
  readonly selectedProperty = signal<Property | null>(null);
  readonly propertyToDelete = signal<PropertySummary | null>(null);
  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly search = signal('');
  readonly status = signal<PropertyStatus | ''>('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly formTitleKey = computed(() => this.formMode() === 'create' ? 'properties.form.createTitle' : 'properties.form.editTitle');

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('properties.pagination.noProperties');
    }

    return this.languageService.instant('properties.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const property = this.propertyToDelete();

    if (!property) {
      return '';
    }

    return this.languageService.instant('properties.confirmDeleteMessage', {
      name: property.name
    });
  });

  constructor(private readonly propertyService: PropertyService) {
  }

  ngOnInit(): void {
    this.loadProperties();
  }

  loadProperties(): void {
    this.loading.set(true);

    this.propertyService.findAll({
      status: this.status(),
      search: this.search().trim(),
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response) => {
        this.applyPage(response);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadProperties();
  }

  clearFilters(): void {
    this.search.set('');
    this.status.set('');
    this.page.set(0);
    this.loadProperties();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadProperties();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadProperties();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadProperties();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedProperty.set(null);
    this.formVisible.set(true);
  }

  openEditForm(propertyId: string): void {
    this.loading.set(true);

    this.propertyService.findById(propertyId).subscribe({
      next: (property) => {
        this.selectedProperty.set(property);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedProperty.set(null);
    this.formMode.set('create');
  }

  saveProperty(request: PropertyRequest): void {
    this.saving.set(true);

    const selectedProperty = this.selectedProperty();

    const saveRequest = this.formMode() === 'edit' && selectedProperty
      ? this.propertyService.update(selectedProperty.id, request)
      : this.propertyService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('properties.messages.updated')
            : this.languageService.instant('properties.messages.created')
        );
        this.closeForm();
        this.loadProperties();
      },
      error: (error) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.messages.saveError')));
      }
    });
  }

  requestDelete(property: PropertySummary): void {
    this.propertyToDelete.set(property);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.propertyToDelete.set(null);
  }

  confirmDelete(): void {
    const property = this.propertyToDelete();

    if (!property) {
      return;
    }

    this.deletingId.set(property.id);

    this.propertyService.delete(property.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.propertyToDelete.set(null);
        this.toastService.success(this.languageService.instant('properties.messages.deleted'));
        this.loadProperties();
      },
      error: (error) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.messages.deleteError')));
      }
    });
  }

  statusBadgeClass(status: PropertyStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'INACTIVE':
        return 'text-bg-secondary';
      case 'DELETED':
        return 'text-bg-danger';
    }
  }

  private applyPage(response: PageResponse<PropertySummary>): void {
    this.properties.set(response.content);
    this.page.set(response.page);
    this.size.set(response.size);
    this.totalElements.set(response.totalElements);
    this.totalPages.set(response.totalPages);
    this.first.set(response.first);
    this.last.set(response.last);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
