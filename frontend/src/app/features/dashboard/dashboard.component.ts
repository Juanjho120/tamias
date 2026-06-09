import { DatePipe, NgClass } from '@angular/common';
import {
  AfterViewChecked,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren,
  computed,
  inject,
  signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Tooltip } from 'bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { ApiError } from '../../core/models/api-error.model';
import { QuetzalCurrencyPipe } from '../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../shared/toast/toast.service';
import {
  DashboardCalendarDay,
  DashboardCalendarRow,
  DashboardData,
  DashboardMetric,
  DashboardReservationCalendarSegment,
  DashboardReservationDetail,
  DashboardTaskListSummary
} from './models/dashboard.model';
import { DashboardService } from './services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, NgClass, RouterLink, TranslatePipe, QuetzalCurrencyPipe],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit, AfterViewChecked, OnDestroy {
  @ViewChildren('reservationTooltip') reservationTooltipElements?: QueryList<ElementRef<HTMLElement>>;

  private readonly dashboardService = inject(DashboardService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  private readonly tooltipInstances = new Map<HTMLElement, Tooltip>();
  private needsTooltipRefresh = false;

  readonly loading = signal(false);
  readonly loadingCalendar = signal(false);
  readonly data = signal<DashboardData | null>(null);

  readonly calendarYear = signal(new Date().getFullYear());
  readonly calendarMonth = signal(new Date().getMonth());
  readonly calendarRows = signal<DashboardCalendarRow[]>([]);

  readonly weekDayKeys = [
    'dashboard.calendar.weekdays.sun',
    'dashboard.calendar.weekdays.mon',
    'dashboard.calendar.weekdays.tue',
    'dashboard.calendar.weekdays.wed',
    'dashboard.calendar.weekdays.thu',
    'dashboard.calendar.weekdays.fri',
    'dashboard.calendar.weekdays.sat'
  ];

  readonly metrics = computed<DashboardMetric[]>(() => {
    const data = this.data();

    return [
      {
        key: 'properties',
        titleKey: 'dashboard.metrics.properties.title',
        descriptionKey: 'dashboard.metrics.properties.description',
        icon: 'bi-houses',
        route: '/properties',
        value: data?.activeProperties ?? 0
      },
      {
        key: 'reservations',
        titleKey: 'dashboard.metrics.reservations.title',
        descriptionKey: 'dashboard.metrics.reservations.description',
        icon: 'bi-calendar2-week',
        route: '/reservations',
        value: data?.activeReservations ?? 0
      },
      {
        key: 'maintenance',
        titleKey: 'dashboard.metrics.maintenance.title',
        descriptionKey: 'dashboard.metrics.maintenance.description',
        icon: 'bi-tools',
        route: '/maintenance',
        value: data?.pendingMaintenance ?? 0
      },
      {
        key: 'scheduledMaintenance',
        titleKey: 'dashboard.metrics.scheduledMaintenance.title',
        descriptionKey: 'dashboard.metrics.scheduledMaintenance.description',
        icon: 'bi-calendar-check',
        route: '/scheduled-maintenance',
        value: data?.dueScheduledMaintenance ?? 0
      },
      {
        key: 'tasks',
        titleKey: 'dashboard.metrics.tasks.title',
        descriptionKey: 'dashboard.metrics.tasks.description',
        icon: 'bi-check2-square',
        route: '/tasks',
        value: data?.openTaskLists ?? 0
      },
      {
        key: 'purchases',
        titleKey: 'dashboard.metrics.purchases.title',
        descriptionKey: 'dashboard.metrics.purchases.description',
        icon: 'bi-cart-check',
        route: '/purchases',
        value: data?.openPurchaseLists ?? 0
      },
      {
        key: 'documents',
        titleKey: 'dashboard.metrics.documents.title',
        descriptionKey: 'dashboard.metrics.documents.description',
        icon: 'bi-file-earmark-text',
        route: '/documents',
        value: (data?.pendingDocuments ?? 0) + (data?.failedDocuments ?? 0)
      },
      {
        key: 'aiAssistant',
        titleKey: 'dashboard.metrics.aiAssistant.title',
        descriptionKey: 'dashboard.metrics.aiAssistant.description',
        icon: 'bi-stars',
        route: '/ai-assistant',
        value: data?.pendingDocuments ?? 0
      }
    ];
  });

  readonly calendarMonthLabel = computed(() => {
    const date = new Date(this.calendarYear(), this.calendarMonth(), 1);

    return date.toLocaleDateString(undefined, {
      month: 'long',
      year: 'numeric'
    });
  });

  ngOnInit(): void {
    this.loadDashboard();
    this.loadCalendar();
  }

  ngAfterViewChecked(): void {
    if (!this.needsTooltipRefresh) {
      return;
    }

    this.needsTooltipRefresh = false;
    this.refreshBootstrapTooltips();
  }

  ngOnDestroy(): void {
    this.disposeTooltips();
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
    this.disposeTooltips();

    this.dashboardService.loadReservationCalendar(
      this.calendarRangeStart(),
      this.calendarRangeEnd()
    ).subscribe({
      next: (reservations) => {
        this.calendarRows.set(this.buildCalendarRows(reservations));
        this.loadingCalendar.set(false);
        this.needsTooltipRefresh = true;
      },
      error: (error: unknown) => {
        this.loadingCalendar.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('dashboard.calendar.loadError')));
      }
    });
  }

  previousMonth(): void {
    const date = new Date(this.calendarYear(), this.calendarMonth() - 1, 1);
    this.calendarYear.set(date.getFullYear());
    this.calendarMonth.set(date.getMonth());
    this.loadCalendar();
  }

  nextMonth(): void {
    const date = new Date(this.calendarYear(), this.calendarMonth() + 1, 1);
    this.calendarYear.set(date.getFullYear());
    this.calendarMonth.set(date.getMonth());
    this.loadCalendar();
  }

  currentMonth(): void {
    const date = new Date();
    this.calendarYear.set(date.getFullYear());
    this.calendarMonth.set(date.getMonth());
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

  calendarSegmentClass(segment: DashboardReservationCalendarSegment): string {
    switch (segment.status) {
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

  calendarSegmentShapeClass(segment: DashboardReservationCalendarSegment): string {
    if (segment.rangeStartsHere && segment.rangeEndsHere) {
      return 'calendar-event-single';
    }

    if (segment.rangeStartsHere) {
      return 'calendar-event-start';
    }

    if (segment.rangeEndsHere) {
      return 'calendar-event-end';
    }

    return 'calendar-event-middle';
  }

  calendarSegmentGridColumn(segment: DashboardReservationCalendarSegment): string {
    return `${segment.gridColumnStart} / ${segment.gridColumnEnd}`;
  }

  calendarRowMinHeightRem(row: DashboardCalendarRow): number {
    return 4.4 + Math.max(1, row.maxLanes) * 1.7;
  }

  invoiceStatusLabel(invoiceStatus: 'INVOICED' | 'NOT_INVOICED'): string {
    return invoiceStatus === 'INVOICED'
      ? this.languageService.instant('dashboard.calendar.invoiced')
      : this.languageService.instant('dashboard.calendar.notInvoiced');
  }

  calendarTooltipHtml(segment: DashboardReservationCalendarSegment): string {
    const rows = [
      [this.languageService.instant('dashboard.calendar.tooltip.guestCount'), String(segment.guestCount)],
      [this.languageService.instant('dashboard.calendar.tooltip.code'), segment.reservationCode || '—'],
      [this.languageService.instant('dashboard.calendar.tooltip.platform'), segment.platformName || '—'],
      [this.languageService.instant('dashboard.calendar.tooltip.invoice'), this.invoiceStatusLabel(segment.invoiceStatus)],
      [this.languageService.instant('dashboard.calendar.tooltip.status'), this.languageService.instant(`reservations.status.${segment.status}`)],
      [this.languageService.instant('dashboard.calendar.tooltip.dates'), `${segment.checkIn} → ${segment.checkOut}`]
    ];

    const rowsHtml = rows.map(([label, value]) => `
      <div class="reservation-calendar-tooltip-row">
        <span>${this.escapeHtml(label)}</span>
        <strong>${this.escapeHtml(value)}</strong>
      </div>
    `).join('');

    return `
      <div class="reservation-calendar-tooltip-content">
        <div class="reservation-calendar-tooltip-title">${this.escapeHtml(segment.primaryGuestName)}</div>
        ${rowsHtml}
        <div class="reservation-calendar-tooltip-property">${this.escapeHtml(segment.propertyName)}</div>
      </div>
    `;
  }

  trackByRow(index: number, row: DashboardCalendarRow): string {
    return row.id;
  }

  trackByDate(index: number, day: DashboardCalendarDay): string {
    return day.date;
  }

  trackByCalendarSegment(index: number, segment: DashboardReservationCalendarSegment): string {
    return segment.id;
  }

  private buildCalendarRows(reservations: DashboardReservationDetail[]): DashboardCalendarRow[] {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const firstDayOfMonth = new Date(year, month, 1);
    const gridStart = new Date(firstDayOfMonth);
    gridStart.setDate(firstDayOfMonth.getDate() - firstDayOfMonth.getDay());

    const today = this.toLocalDateString(new Date());
    const rows: DashboardCalendarRow[] = [];

    for (let rowIndex = 0; rowIndex < 6; rowIndex++) {
      const days: DashboardCalendarDay[] = [];

      for (let dayIndex = 0; dayIndex < 7; dayIndex++) {
        const date = new Date(gridStart);
        date.setDate(gridStart.getDate() + rowIndex * 7 + dayIndex);

        const dateString = this.toLocalDateString(date);

        days.push({
          date: dateString,
          dayNumber: date.getDate(),
          currentMonth: date.getMonth() === month,
          today: dateString === today
        });
      }

      const segments = this.segmentsForCalendarRow(days, reservations);

      rows.push({
        id: days[0].date,
        days,
        segments,
        maxLanes: segments.length === 0 ? 1 : Math.max(...segments.map((segment) => segment.lane)) + 1
      });
    }

    return rows;
  }

  private segmentsForCalendarRow(days: DashboardCalendarDay[], reservations: DashboardReservationDetail[]): DashboardReservationCalendarSegment[] {
    const rowStart = days[0].date;
    const rowEnd = days[6].date;

    const overlappingReservations = reservations
      .map((reservation) => {
        const lastStayDate = this.previousDateString(reservation.checkOut);
        const segmentStart = reservation.checkIn > rowStart ? reservation.checkIn : rowStart;
        const segmentEnd = lastStayDate < rowEnd ? lastStayDate : rowEnd;

        return {
          reservation,
          lastStayDate,
          segmentStart,
          segmentEnd
        };
      })
      .filter((item) => item.segmentStart <= item.segmentEnd)
      .sort((left, right) => {
        const byStart = left.segmentStart.localeCompare(right.segmentStart);

        if (byStart !== 0) {
          return byStart;
        }

        return right.segmentEnd.localeCompare(left.segmentEnd);
      });

    const laneEndIndexes: number[] = [];

    return overlappingReservations.map((item) => {
      const startIndex = days.findIndex((day) => day.date === item.segmentStart);
      const endIndex = days.findIndex((day) => day.date === item.segmentEnd);
      const lane = this.findAvailableLane(laneEndIndexes, startIndex);

      laneEndIndexes[lane] = endIndex;

      return this.toCalendarSegment(item.reservation, item.lastStayDate, item.segmentStart, item.segmentEnd, startIndex, endIndex, lane);
    });
  }

  private findAvailableLane(laneEndIndexes: number[], startIndex: number): number {
    const existingLane = laneEndIndexes.findIndex((endIndex) => endIndex < startIndex);

    if (existingLane >= 0) {
      return existingLane;
    }

    return laneEndIndexes.length;
  }

  private toCalendarSegment(
    reservation: DashboardReservationDetail,
    lastStayDate: string,
    segmentStart: string,
    segmentEnd: string,
    startIndex: number,
    endIndex: number,
    lane: number
  ): DashboardReservationCalendarSegment {
    const guests = reservation.guests ?? [];
    const primaryGuest = guests.find((guest) => guest.primary) ?? guests[0] ?? null;
    const invoiceStatus = reservation.invoiceNumber || reservation.invoiceSeries ? 'INVOICED' : 'NOT_INVOICED';

    return {
      id: `${reservation.id}-${segmentStart}-${segmentEnd}`,
      reservationId: reservation.id,
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
      rangeStartsHere: reservation.checkIn === segmentStart,
      rangeEndsHere: lastStayDate === segmentEnd,
      gridColumnStart: startIndex + 1,
      gridColumnEnd: endIndex + 2,
      lane,
      topRem: 2.35 + lane * 1.55
    };
  }

  private calendarRangeStart(): string {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const firstDayOfMonth = new Date(year, month, 1);
    const gridStart = new Date(firstDayOfMonth);
    gridStart.setDate(firstDayOfMonth.getDate() - firstDayOfMonth.getDay());

    return this.toLocalDateString(gridStart);
  }

  private calendarRangeEnd(): string {
    const year = this.calendarYear();
    const month = this.calendarMonth();
    const lastDayOfMonth = new Date(year, month + 1, 0);
    const gridEnd = new Date(lastDayOfMonth);
    gridEnd.setDate(lastDayOfMonth.getDate() + (6 - lastDayOfMonth.getDay()) + 1);

    return this.toLocalDateString(gridEnd);
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

  private refreshBootstrapTooltips(): void {
    this.disposeTooltips();

    const elements = this.reservationTooltipElements?.toArray() ?? [];

    for (const elementRef of elements) {
      const element = elementRef.nativeElement;
      const tooltip = new Tooltip(element, {
        html: true,
        placement: 'top',
        trigger: 'hover focus',
        container: 'body',
        customClass: 'reservation-calendar-bootstrap-tooltip'
      });

      this.tooltipInstances.set(element, tooltip);
    }
  }

  private disposeTooltips(): void {
    for (const tooltip of this.tooltipInstances.values()) {
      tooltip.dispose();
    }

    this.tooltipInstances.clear();
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
