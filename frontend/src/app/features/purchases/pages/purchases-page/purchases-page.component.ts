import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { QuetzalCurrencyPipe } from '../../../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PurchaseItemsModalComponent } from '../../components/purchase-items-modal/purchase-items-modal.component';
import { PurchaseListFormModalComponent } from '../../components/purchase-list-form-modal/purchase-list-form-modal.component';
import {
  PurchaseList,
  PurchaseListRequest,
  PurchaseListStatus,
  PurchaseListSummary,
  PURCHASE_LIST_STATUSES
} from '../../models/purchase-list.model';
import { PurchaseReferenceData, PurchaseReferenceDataService } from '../../services/purchase-reference-data.service';
import { PurchaseListService } from '../../services/purchase-list.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-purchases-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    QuetzalCurrencyPipe,
    PurchaseItemsModalComponent,
    PurchaseListFormModalComponent
  ],
  templateUrl: './purchases-page.component.html'
})
export class PurchasesPageComponent implements OnInit {
  private readonly purchaseListService = inject(PurchaseListService);
  private readonly referenceDataService = inject(PurchaseReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = PURCHASE_LIST_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);

  readonly purchaseLists = signal<PurchaseListSummary[]>([]);
  readonly selectedPurchaseList = signal<PurchaseList | null>(null);
  readonly purchaseListForItems = signal<PurchaseListSummary | null>(null);
  readonly purchaseListToDelete = signal<PurchaseListSummary | null>(null);

  readonly references = signal<PurchaseReferenceData>({
    properties: [],
    cities: [],
    suppliers: [],
    materials: [],
    brands: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly propertyId = signal('');
  readonly supplierId = signal('');
  readonly cityId = signal('');
  readonly status = signal<PurchaseListStatus | ''>('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('purchases.pagination.noItems');
    }

    return this.languageService.instant('purchases.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const purchaseList = this.purchaseListToDelete();

    if (!purchaseList) {
      return '';
    }

    return this.languageService.instant('purchases.confirmDeleteMessage', {
      date: purchaseList.purchaseDate
    });
  });

  ngOnInit(): void {
    this.loadReferences();
    this.loadPurchaseLists();
  }

  loadReferences(): void {
    this.loadingReferences.set(true);

    this.referenceDataService.loadAll().subscribe({
      next: (references) => {
        this.references.set(references);
        this.loadingReferences.set(false);
      },
      error: (error: unknown) => {
        this.loadingReferences.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.messages.referencesError')));
      }
    });
  }

  loadPurchaseLists(): void {
    this.loading.set(true);

    this.purchaseListService.findAll({
      propertyId: this.propertyId() || undefined,
      supplierId: this.supplierId() || undefined,
      cityId: this.cityId() || undefined,
      status: this.status(),
      page: this.page(),
      size: this.size(),
      sort: 'purchaseDate,desc'
    }).subscribe({
      next: (response: PageResponse<PurchaseListSummary>) => {
        this.purchaseLists.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadPurchaseLists();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.supplierId.set('');
    this.cityId.set('');
    this.status.set('');
    this.page.set(0);
    this.loadPurchaseLists();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadPurchaseLists();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadPurchaseLists();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadPurchaseLists();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedPurchaseList.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.purchaseListService.findById(id).subscribe({
      next: (purchaseList) => {
        this.selectedPurchaseList.set(purchaseList);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedPurchaseList.set(null);
    this.formMode.set('create');
  }

  savePurchaseList(request: PurchaseListRequest): void {
    const selectedPurchaseList = this.selectedPurchaseList();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedPurchaseList
      ? this.purchaseListService.update(selectedPurchaseList.id, request)
      : this.purchaseListService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('purchases.messages.updated')
            : this.languageService.instant('purchases.messages.created')
        );
        this.closeForm();
        this.loadPurchaseLists();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.messages.saveError')));
      }
    });
  }

  openItems(purchaseList: PurchaseListSummary): void {
    this.purchaseListForItems.set(purchaseList);
  }

  closeItems(): void {
    this.purchaseListForItems.set(null);
  }

  requestDelete(purchaseList: PurchaseListSummary): void {
    this.purchaseListToDelete.set(purchaseList);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.purchaseListToDelete.set(null);
  }

  confirmDelete(): void {
    const purchaseList = this.purchaseListToDelete();

    if (!purchaseList) {
      return;
    }

    this.deletingId.set(purchaseList.id);

    this.purchaseListService.delete(purchaseList.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.purchaseListToDelete.set(null);
        this.toastService.success(this.languageService.instant('purchases.messages.deleted'));
        this.loadPurchaseLists();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.messages.deleteError')));
      }
    });
  }

  statusBadgeClass(status: PurchaseListStatus): string {
    switch (status) {
      case 'OPEN':
        return 'text-bg-secondary';
      case 'PARTIALLY_PURCHASED':
        return 'text-bg-info';
      case 'COMPLETED':
        return 'text-bg-success';
      case 'CANCELLED':
        return 'text-bg-warning';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  purchasedRatio(purchaseList: PurchaseListSummary): number {
    if (!purchaseList.totalItems) {
      return 0;
    }

    return purchaseList.purchasedItems / purchaseList.totalItems;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
