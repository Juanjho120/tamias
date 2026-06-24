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
import { PaymentFormModalComponent } from '../../components/payment-form-modal/payment-form-modal.component';
import { PaymentImagesModalComponent } from '../../components/payment-images-modal/payment-images-modal.component';
import { Payment, PaymentMethod, PaymentRequest, PAYMENT_METHODS } from '../../models/payment.model';
import { PaymentReferenceData, PaymentReferenceDataService } from '../../services/payment-reference-data.service';
import { PaymentService } from '../../services/payment.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-payments-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    QuetzalCurrencyPipe,
    PaymentFormModalComponent,
    PaymentImagesModalComponent
  ],
  templateUrl: './payments-page.component.html'
})
export class PaymentsPageComponent implements OnInit {
  private readonly paymentService = inject(PaymentService);
  private readonly referenceDataService = inject(PaymentReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly methods = PAYMENT_METHODS;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);

  readonly payments = signal<Payment[]>([]);
  readonly selectedPayment = signal<Payment | null>(null);
  readonly paymentForImages = signal<Payment | null>(null);
  readonly paymentToDelete = signal<Payment | null>(null);

  readonly references = signal<PaymentReferenceData>({
    properties: [],
    categories: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly propertyId = signal('');
  readonly categoryId = signal('');
  readonly method = signal<PaymentMethod | ''>('');
  readonly dateFrom = signal('');
  readonly dateTo = signal('');
  readonly search = signal('');

  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('payments.pagination.noItems');
    }

    return this.languageService.instant('payments.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly visibleTotal = computed(() =>
    this.payments().reduce((total, payment) => total + (payment.amount ?? 0), 0)
  );

  readonly deleteMessage = computed(() => {
    const payment = this.paymentToDelete();

    if (!payment) {
      return '';
    }

    return this.languageService.instant('payments.confirmDeleteMessage', {
      name: payment.name,
      date: payment.payDate
    });
  });

  ngOnInit(): void {
    this.loadReferences();
    this.loadPayments();
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
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.messages.referencesError'))
        );
      }
    });
  }

  loadPayments(): void {
    this.loading.set(true);

    this.paymentService
      .findAll({
        propertyId: this.propertyId() || undefined,
        categoryId: this.categoryId() || undefined,
        method: this.method() || undefined,
        dateFrom: this.dateFrom() || undefined,
        dateTo: this.dateTo() || undefined,
        search: this.search().trim() || undefined,
        page: this.page(),
        size: this.size(),
        sort: 'payDate,desc'
      })
      .subscribe({
        next: (response: PageResponse<Payment>) => {
          this.payments.set(response.content);
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
          this.toastService.error(
            this.extractErrorMessage(error, this.languageService.instant('payments.messages.loadError'))
          );
        }
      });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadPayments();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.categoryId.set('');
    this.method.set('');
    this.dateFrom.set('');
    this.dateTo.set('');
    this.search.set('');
    this.page.set(0);
    this.loadPayments();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadPayments();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadPayments();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadPayments();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedPayment.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.paymentService.findById(id).subscribe({
      next: (payment) => {
        this.selectedPayment.set(payment);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.messages.detailError'))
        );
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedPayment.set(null);
    this.formMode.set('create');
  }

  savePayment(request: PaymentRequest): void {
    const selectedPayment = this.selectedPayment();

    this.saving.set(true);

    const saveRequest =
      this.formMode() === 'edit' && selectedPayment
        ? this.paymentService.update(selectedPayment.id, request)
        : this.paymentService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('payments.messages.updated')
            : this.languageService.instant('payments.messages.created')
        );
        this.closeForm();
        this.loadPayments();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.messages.saveError'))
        );
      }
    });
  }

  openImages(payment: Payment): void {
    this.paymentForImages.set(payment);
  }

  closeImages(): void {
    this.paymentForImages.set(null);
  }

  requestDelete(payment: Payment): void {
    this.paymentToDelete.set(payment);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.paymentToDelete.set(null);
  }

  confirmDelete(): void {
    const payment = this.paymentToDelete();

    if (!payment) {
      return;
    }

    this.deletingId.set(payment.id);

    this.paymentService.delete(payment.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.paymentToDelete.set(null);
        this.toastService.success(this.languageService.instant('payments.messages.deleted'));
        this.loadPayments();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.messages.deleteError'))
        );
      }
    });
  }

  methodBadgeClass(method: PaymentMethod): string {
    switch (method) {
      case 'CREDIT':
        return 'text-bg-primary';
      case 'DEBIT':
        return 'text-bg-info';
      case 'CASH':
        return 'text-bg-success';
      case 'BANK_TRANSFER':
        return 'text-bg-secondary';
      default:
        return 'text-bg-secondary';
    }
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
