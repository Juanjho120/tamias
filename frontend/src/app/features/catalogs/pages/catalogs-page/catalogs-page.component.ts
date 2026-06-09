import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { CatalogFormModalComponent } from '../../components/catalog-form-modal/catalog-form-modal.component';
import { CATALOG_CONFIGS } from '../../catalogs.config';
import { CatalogConfig, CatalogFieldConfig, CatalogItem, CatalogRequest, CatalogStatus, CATALOG_STATUSES } from '../../models/catalog.model';
import { CatalogService } from '../../services/catalog.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-catalogs-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    CatalogFormModalComponent,
    ConfirmModalComponent
  ],
  templateUrl: './catalogs-page.component.html'
})
export class CatalogsPageComponent implements OnInit {
  private readonly catalogService = inject(CatalogService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly configs = CATALOG_CONFIGS;
  readonly statuses = CATALOG_STATUSES;

  readonly selectedConfig = signal<CatalogConfig>(CATALOG_CONFIGS[0]);
  readonly items = signal<CatalogItem[]>([]);
  readonly selectedItem = signal<CatalogItem | null>(null);
  readonly itemToDelete = signal<CatalogItem | null>(null);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly status = signal<CatalogStatus | ''>('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly tableFields = computed(() => this.selectedConfig().fields.filter((field) => field.table));
  readonly primaryField = computed(() => this.selectedConfig().fields.find((field) => field.primary) ?? this.selectedConfig().fields[0]);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('catalogs.pagination.noItems');
    }

    return this.languageService.instant('catalogs.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const item = this.itemToDelete();

    if (!item) {
      return '';
    }

    return this.languageService.instant('catalogs.confirmDeleteMessage', {
      name: this.displayName(item)
    });
  });

  ngOnInit(): void {
    this.loadItems();
  }

  selectCatalog(config: CatalogConfig): void {
    if (this.selectedConfig().key === config.key) {
      return;
    }

    this.selectedConfig.set(config);
    this.status.set('');
    this.page.set(0);
    this.formVisible.set(false);
    this.selectedItem.set(null);
    this.itemToDelete.set(null);
    this.loadItems();
  }

  loadItems(): void {
    const config = this.selectedConfig();

    this.loading.set(true);

    this.catalogService.findAll(config.endpoint, {
      status: this.status(),
      page: this.page(),
      size: this.size(),
      sort: config.defaultSort ?? 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<CatalogItem>) => {
        this.items.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('catalogs.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadItems();
  }

  clearFilters(): void {
    this.status.set('');
    this.page.set(0);
    this.loadItems();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadItems();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadItems();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadItems();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedItem.set(null);
    this.formVisible.set(true);
  }

  openEditForm(item: CatalogItem): void {
    this.formMode.set('edit');
    this.selectedItem.set(item);
    this.formVisible.set(true);
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedItem.set(null);
    this.formMode.set('create');
  }

  saveItem(request: CatalogRequest): void {
    const config = this.selectedConfig();
    const selectedItem = this.selectedItem();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedItem
      ? this.catalogService.update(config.endpoint, selectedItem.id, request)
      : this.catalogService.create(config.endpoint, request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('catalogs.messages.updated')
            : this.languageService.instant('catalogs.messages.created')
        );
        this.closeForm();
        this.loadItems();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('catalogs.messages.saveError')));
      }
    });
  }

  requestDelete(item: CatalogItem): void {
    this.itemToDelete.set(item);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.itemToDelete.set(null);
  }

  confirmDelete(): void {
    const config = this.selectedConfig();
    const item = this.itemToDelete();

    if (!item) {
      return;
    }

    this.deletingId.set(item.id);

    this.catalogService.delete(config.endpoint, item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.itemToDelete.set(null);
        this.toastService.success(this.languageService.instant('catalogs.messages.deleted'));
        this.loadItems();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('catalogs.messages.deleteError')));
      }
    });
  }

  displayValue(item: CatalogItem, field: CatalogFieldConfig): string {
    const value = item[field.key as keyof CatalogItem];

    if (value === null || value === undefined || value === '') {
      return '—';
    }

    return String(value);
  }

  displayName(item: CatalogItem): string {
    const primary = this.primaryField();
    return this.displayValue(item, primary);
  }

  statusBadgeClass(status: CatalogStatus): string {
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

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
