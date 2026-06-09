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
import { CancelReservationModalComponent } from '../../components/cancel-reservation-modal/cancel-reservation-modal.component';
import { ReservationFormModalComponent } from '../../components/reservation-form-modal/reservation-form-modal.component';
import {
  Reservation,
  ReservationRequest,
  ReservationStatus,
  ReservationSummary,
  RESERVATION_STATUSES
} from '../../models/reservation.model';
import { ReservationReferenceData, ReservationReferenceDataService } from '../../services/reservation-reference-data.service';
import { ReservationService } from '../../services/reservation.service';

type FormMode = 'create' | 'edit';
type ViewMode = 'list' | 'calendar';

@Component({
  selector: 'app-reservations-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    QuetzalCurrencyPipe,
    CancelReservationModalComponent,
    ReservationFormModalComponent
  ],
  templateUrl: './reservations-page.component.html'
})
export class ReservationsPageComponent implements OnInit {
  private readonly reservationService = inject(ReservationService);
  private readonly referenceDataService = inject(ReservationReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = RESERVATION_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly cancelling = signal(false);
  readonly loadingReferences = signal(false);

  readonly reservations = signal<ReservationSummary[]>([]);
  readonly selectedReservation = signal<Reservation | null>(null);
  readonly reservationToDelete = signal<ReservationSummary | null>(null);
  readonly reservationToCancel = signal<ReservationSummary | null>(null);

  readonly references = signal<ReservationReferenceData>({
    properties: [],
    platforms: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly viewMode = signal<ViewMode>('list');

  readonly propertyId = signal('');
  readonly status = signal<ReservationStatus | ''>('');
  readonly startDate = signal(this.defaultStartDate());
  readonly endDate = signal(this.defaultEndDate());
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('reservations.pagination.noItems');
    }

    return this.languageService.instant('reservations.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const reservation = this.reservationToDelete();

    if (!reservation) {
      return '';
    }

    return this.languageService.instant('reservations.confirmDeleteMessage', {
      code: this.reservationDisplayName(reservation)
    });
  });

  readonly cancelReservationTitle = computed(() => {
    const reservation = this.reservationToCancel();
    return reservation ? this.reservationDisplayName(reservation) : '';
  });

  ngOnInit(): void {
    this.loadReferences();
    this.loadReservations();
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.referencesError')));
      }
    });
  }

  loadReservations(): void {
    this.loading.set(true);

    const request = this.viewMode() === 'calendar'
      ? this.reservationService.findCalendar({
        startDate: this.startDate(),
        endDate: this.endDate(),
        page: this.page(),
        size: this.size(),
        sort: 'checkIn,asc'
      })
      : this.reservationService.findAll({
        propertyId: this.propertyId() || undefined,
        status: this.status(),
        page: this.page(),
        size: this.size(),
        sort: 'checkIn,desc'
      });

    request.subscribe({
      next: (response: PageResponse<ReservationSummary>) => {
        this.reservations.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.loadError')));
      }
    });
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
    this.page.set(0);
    this.loadReservations();
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadReservations();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.status.set('');
    this.startDate.set(this.defaultStartDate());
    this.endDate.set(this.defaultEndDate());
    this.page.set(0);
    this.loadReservations();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadReservations();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadReservations();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadReservations();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedReservation.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.reservationService.findById(id).subscribe({
      next: (reservation: Reservation) => {
        this.selectedReservation.set(reservation);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedReservation.set(null);
    this.formMode.set('create');
  }

  saveReservation(request: ReservationRequest): void {
    const selectedReservation = this.selectedReservation();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedReservation
      ? this.reservationService.update(selectedReservation.id, request)
      : this.reservationService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('reservations.messages.updated')
            : this.languageService.instant('reservations.messages.created')
        );
        this.closeForm();
        this.loadReservations();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.saveError')));
      }
    });
  }

  requestCancel(reservation: ReservationSummary): void {
    this.reservationToCancel.set(reservation);
  }

  closeCancel(): void {
    if (this.cancelling()) {
      return;
    }

    this.reservationToCancel.set(null);
  }

  confirmCancel(payload: { reason: string | null }): void {
    const reservation = this.reservationToCancel();

    if (!reservation) {
      return;
    }

    this.cancelling.set(true);

    this.reservationService.cancel(reservation.id, payload).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.reservationToCancel.set(null);
        this.toastService.success(this.languageService.instant('reservations.messages.cancelled'));
        this.loadReservations();
      },
      error: (error: unknown) => {
        this.cancelling.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.cancelError')));
      }
    });
  }

  requestDelete(reservation: ReservationSummary): void {
    this.reservationToDelete.set(reservation);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.reservationToDelete.set(null);
  }

  confirmDelete(): void {
    const reservation = this.reservationToDelete();

    if (!reservation) {
      return;
    }

    this.deletingId.set(reservation.id);

    this.reservationService.delete(reservation.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.reservationToDelete.set(null);
        this.toastService.success(this.languageService.instant('reservations.messages.deleted'));
        this.loadReservations();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.messages.deleteError')));
      }
    });
  }

  statusBadgeClass(status: ReservationStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'CANCELLED':
        return 'text-bg-warning';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  reservationDisplayName(reservation: ReservationSummary): string {
    return reservation.reservationCode || `${reservation.propertyName} ${reservation.checkIn}`;
  }

  guestNamesLabel(reservation: ReservationSummary): string {
    return reservation.guestNames?.length ? reservation.guestNames.join(', ') : '—';
  }

  canCancel(reservation: ReservationSummary): boolean {
    return reservation.status === 'ACTIVE';
  }

  private defaultStartDate(): string {
    const date = new Date();
    date.setDate(1);
    return date.toISOString().slice(0, 10);
  }

  private defaultEndDate(): string {
    const date = new Date();
    date.setMonth(date.getMonth() + 2);
    return date.toISOString().slice(0, 10);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
