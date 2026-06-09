import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { Property, PropertyRequest, PropertyStatus, PropertySummary, PROPERTY_STATUSES } from '../../models/property.model';
import { PropertyService } from '../../services/property.service';
import { PropertyFormComponent } from '../../components/property-form/property-form.component';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-properties-page',
  standalone: true,
  imports: [DatePipe, FormsModule, NgClass, PropertyFormComponent],
  templateUrl: './properties-page.component.html'
})
export class PropertiesPageComponent implements OnInit {
  readonly statuses = PROPERTY_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly properties = signal<PropertySummary[]>([]);
  readonly selectedProperty = signal<Property | null>(null);
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

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return 'No properties';
    }

    return `Page ${this.page() + 1} of ${this.totalPages()}`;
  });

  constructor(private readonly propertyService: PropertyService) {
  }

  ngOnInit(): void {
    this.loadProperties();
  }

  loadProperties(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

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
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to load properties.'));
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
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.formMode.set('create');
    this.selectedProperty.set(null);
    this.formVisible.set(true);
  }

  openEditForm(propertyId: string): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
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
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to load property details.'));
      }
    });
  }

  closeForm(): void {
    this.formVisible.set(false);
    this.selectedProperty.set(null);
    this.formMode.set('create');
  }

  saveProperty(request: PropertyRequest): void {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const selectedProperty = this.selectedProperty();

    const saveRequest = this.formMode() === 'edit' && selectedProperty
      ? this.propertyService.update(selectedProperty.id, request)
      : this.propertyService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set(this.formMode() === 'edit' ? 'Property updated successfully.' : 'Property created successfully.');
        this.closeForm();
        this.loadProperties();
      },
      error: (error) => {
        this.saving.set(false);
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to save property.'));
      }
    });
  }

  deleteProperty(property: PropertySummary): void {
    const confirmed = window.confirm(`Delete property "${property.name}"?`);

    if (!confirmed) {
      return;
    }

    this.deletingId.set(property.id);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.propertyService.delete(property.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.successMessage.set('Property deleted successfully.');
        this.loadProperties();
      },
      error: (error) => {
        this.deletingId.set(null);
        this.errorMessage.set(this.extractErrorMessage(error, 'Unable to delete property.'));
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
