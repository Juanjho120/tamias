import { DatePipe, NgClass, PercentPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { ApiError } from '../../core/models/api-error.model';
import { QuetzalCurrencyPipe } from '../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../shared/toast/toast.service';
import {
  DashboardCalendarDay,
  DashboardData,
  DashboardMetric,
  DashboardReservationCalendarEvent,
  DashboardReservationDetail,
  DashboardTaskListSummary
} from './models/dashboard.model';
import { DashboardService } from './services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, NgClass, PercentPipe, RouterLink, TranslatePipe, QuetzalCurrencyPipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal(false);
  readonly loadingCalendar = signal(false);
  readonly data = signal<DashboardData | null>(null);

  readonly calendarYear = signal(new Date().getFullYear());
  readonly calendarMonth = signal(new Date().getMonth());
  readonly calendarDays = signal<DashboardCalendarDay[]>([]);

  readonly weekDayKeys = [
    'dashboard.calendar.weekDays.sun',
    'dashboard.calendar.weekDays.mon',
    'dashboard.calendar.weekDays.tue',
    'dashboard.calendar.weekDays.wed',
    'dashboard.calendar.weekDays.thu',
    'dashboard.calendar.weekDays.fri',
    'dashboard.calendar.weekDays.sat'
  ];

  readonly calendarMonthLabel = computed(() => {
    const date = new Date(this.calendarYear(), this.calendarMonth(), 1);
    return date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  });

  readonly calendarWeeks = computed(() => {
    const days = this.calendarDays();
    const weeks: DashboardCalendarDay[][] = [];

    for (let index = 0; index < days.length; index += 7) {
      weeks.push(days.slice(index, index + 7));
    }

    return weeks;
  });

  readonly metrics = computed<DashboardMetric[]>(() => {
    const data = this.data();

    return [
      { key: 'properties', titleKey: 'dashboard.metrics.properties.title', descriptionKey: 'dashboard.metrics.properties.description', icon: 'bi-houses', route: '/properties', value: data?.activeProperties ?? 0 },
      { key: 'reservations', titleKey: 'dashboard.metrics.reservations.title', descriptionKey: 'dashboard.metrics.reservations.description', icon: 'bi-calendar2-week', route: '/reservations', value: data?.activeReservations ?? 0 },
      { key: 'maintenance', titleKey: 'dashboard.metrics.maintenance.title', descriptionKey: 'dashboard.metrics.maintenance.description', icon: 'bi-tools', route: '/maintenance', value: data?.pendingMaintenance ?? 0 },
      { key: 'scheduledMaintenance', titleKey: 'dashboard.metrics.scheduledMaintenance.title', descriptionKey: 'dashboard.metrics.scheduledMaintenance.description', icon: 'bi-calendar-check', route: '/scheduled-maintenance', value: data?.dueScheduledMaintenance ?? 0 },
      { key: 'tasks', titleKey: 'dashboard.metrics.tasks.title', descriptionKey: 'dashboard.metrics.tasks.description', icon: 'bi-check2-square', route: '/tasks', value: data?.openTaskLists ?? 0 },
      { key: 'purchases', titleKey: 'dashboard.metrics.purchases.title', descriptionKey: 'dashboard.metrics.purchases.description', icon: 'bi-cart-check', route: '/purchases', value: data?.openPurchaseLists ?? 0 },
      { key: 'documents', titleKey: 'dashboard.metrics.documents.title', descriptionKey: 'dashboard.metrics.documents.description', icon: 'bi-file-earmark-text', route: '/documents', value: (data?.pendingDocuments ?? 0) + (data?.failedDocuments ?? 0) },
      { key: 'aiAssistant', titleKey: 'dashboard.metrics.aiAssistant.title', descriptionKey: 'dashboard.metrics.aiAssistant.description', icon: 'bi-stars', route: '/ai-assistant', value: data?.pendingDocuments ?? 0 }
    ];
  });

  ngOnInit(): void {
    this.loadDashboard();
    this.loadCalendar();
  }

  loadDashboard(): void {
    this.loading.set(true);

    this.dashboardService.loadDashboard().subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('dashboard.messages.loadError')));
      }
    });
  }

  loadCalendar(): void {
    this.loadingCalendar.set(true);

    this.dashboardService.loadReservationDetailsForMonth(this.calendarYear(), this.calendarMonth()).subscribe({
      next: (reservations) => {
        this.calendarDays.set(this.buildCalendarDays(reservations));
        this.loadingCalendar.set(false);
      },
      error: (error: unknown) => {
        this.loadingCalendar.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('dashboard.calendar.messages.loadError')));
      }
    });
  }

  previousMonth(): void {
    const current = new Date(this.calendarYear(), this.calendarMonth(), 1);
    current.setMonth(current.getMonth() - 1);
    this.calendarYear.set(current.getFullYear());
    this.calendarMonth.set(current.getMonth());
    this.loadCalendar();
  }

  nextMonth(): void {
    const current = new Date(this.calendarYear(), this.calendarMonth(), 1);
    current.setMonth(current.getMonth() + 1);
    this.calendarYear.set(current.getFullYear());
    this.calendarMonth.set(current.getMonth());
    this.loadCalendar();
  }

  currentMonth(): void {
    const today = new Date();
    this.calendarYear.set(today.getFullYear());
    this.calendarMonth.set(today.getMonth());
    this.loadCalendar();
  }

  taskCompletionRatio(taskList: DashboardTaskListSummary): number {
    if (!taskList.totalItems) {
      return 0;
    }

    return taskList.completedItems / taskList.totalItems;
  }

  badgeClass(status: string): string {
    switch (status) {
      case 'ACTIVE':
      case 'COMPLETED':
      case 'PROCESSED':
        return 'text-bg-success';
      case 'OPEN':
      case 'PENDING':
        return 'text-bg-secondary';
      case 'IN_PROGRESS':
      case 'PROCESSING':
      case 'PARTIALLY_PURCHASED':
        return 'text-bg-info';
      case 'CANCELLED':
        return 'text-bg-warning';
      case 'FAILED':
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  calendarEventClass(event: DashboardReservationCalendarEvent): string {
    switch (event.status) {
      case 'ACTIVE':
        return 'calendar-event-active';
      case 'CANCELLED':
        return 'calendar-event-cancelled';
      case 'DELETED':
        return 'calendar-event-deleted';
      default:
        return 'calendar-event-default';
    }
  }

  calendarEventShapeClass(event: DashboardReservationCalendarEvent): string {
    if (event.startsOnDate && event.endsOnDate) {
      return 'calendar-event-single';
    }

    if (event.startsOnDate) {
      return 'calendar-event-start';
    }

    if (event.endsOnDate) {
      return 'calendar-event-end';
    }

    return 'calendar-event-middle';
  }

  trackByDate(index: number, day: DashboardCalendarDay): string {
    return day.date;
  }

  trackByCalendarEvent(index: number, event: DashboardReservationCalendarEvent): string {
    return `${event.id}-${event.startsOnDate}-${event.endsOnDate}-${index}`;
  }

  private buildCalendarDays(reservations: DashboardReservationDetail[]): DashboardCalendarDay[] {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const firstDayOfMonth = new Date(year, month, 1);
    const gridStart = new Date(firstDayOfMonth);
    gridStart.setDate(firstDayOfMonth.getDate() - firstDayOfMonth.getDay());

    const today = this.toLocalDateString(new Date());
    const days: DashboardCalendarDay[] = [];

    for (let index = 0; index < 42; index++) {
      const date = new Date(gridStart);
      date.setDate(gridStart.getDate() + index);
      const dateString = this.toLocalDateString(date);

      days.push({
        date: dateString,
        dayNumber: date.getDate(),
        currentMonth: date.getMonth() === month,
        today: dateString === today,
        events: this.eventsForDate(dateString, reservations)
      });
    }

    return days;
  }

  private eventsForDate(date: string, reservations: DashboardReservationDetail[]): DashboardReservationCalendarEvent[] {
    return reservations
      .filter((reservation) => this.dateIsWithinStay(date, reservation))
      .map((reservation) => this.toCalendarEvent(date, reservation))
      .sort((left, right) => left.checkIn.localeCompare(right.checkIn));
  }

  private dateIsWithinStay(date: string, reservation: DashboardReservationDetail): boolean {
    return date >= reservation.checkIn && date < reservation.checkOut;
  }

  private toCalendarEvent(date: string, reservation: DashboardReservationDetail): DashboardReservationCalendarEvent {
    const guests = reservation.guests ?? [];
    const primaryGuest = guests.find((guest) => guest.primary) ?? guests[0] ?? null;
    const invoiceStatus = reservation.invoiceNumber || reservation.invoiceSeries ? 'INVOICED' : 'NOT_INVOICED';
    const lastStayDate = this.previousDateString(reservation.checkOut);

    return {
      id: reservation.id,
      propertyId: reservation.propertyId,
      propertyName: reservation.propertyName,
      platformName: reservation.platformName,
      reservationCode: reservation.reservationCode,
      checkIn: reservation.checkIn,
      checkOut: reservation.checkOut,
      guestNames: guests.map((guest) => guest.fullName).filter((name): name is string => !!name),
      primaryGuestName: primaryGuest?.fullName ?? this.languageService.instant('dashboard.calendar.noGuest'),
      guestCount: guests.length,
      invoiceStatus,
      status: reservation.status,
      startsOnDate: date === reservation.checkIn,
      endsOnDate: date === lastStayDate,
      tooltip: this.buildReservationTooltip(reservation, primaryGuest?.fullName ?? null, guests.length, invoiceStatus)
    };
  }

  private buildReservationTooltip(
    reservation: DashboardReservationDetail,
    primaryGuestName: string | null,
    guestCount: number,
    invoiceStatus: 'INVOICED' | 'NOT_INVOICED'
  ): string {
    const invoiceLabel = invoiceStatus === 'INVOICED'
      ? this.languageService.instant('dashboard.calendar.invoiced')
      : this.languageService.instant('dashboard.calendar.notInvoiced');

    return [
      `${this.languageService.instant('dashboard.calendar.tooltip.guest')}: ${primaryGuestName ?? this.languageService.instant('dashboard.calendar.noGuest')}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.guestCount')}: ${guestCount}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.code')}: ${reservation.reservationCode ?? '—'}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.platform')}: ${reservation.platformName ?? '—'}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.invoice')}: ${invoiceLabel}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.status')}: ${reservation.status}`,
      `${this.languageService.instant('dashboard.calendar.tooltip.dates')}: ${reservation.checkIn} → ${reservation.checkOut}`
    ].join('\\n');
  }

  private previousDateString(date: string): string {
    const value = new Date(`${date}T00:00:00`);
    value.setDate(value.getDate() - 1);
    return this.toLocalDateString(value);
  }

  private toLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
