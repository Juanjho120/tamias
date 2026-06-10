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
  effect,
  inject,
  signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import Tooltip from 'bootstrap/js/dist/tooltip';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { ApiError } from '../../core/models/api-error.model';
import { QuetzalCurrencyPipe } from '../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../shared/toast/toast.service';
import {
  DashboardCalendarData,
  DashboardCalendarDay,
  DashboardCalendarDayIcon,
  DashboardCalendarRow,
  DashboardData,
  DashboardMetric,
  DashboardReservationCalendarSegment,
  DashboardReservationDetail,
  DashboardScheduledMaintenanceCalendarItem,
  DashboardScheduledMaintenanceCalendarSegment,
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
  @ViewChildren('calendarTooltip') calendarTooltipElements?: QueryList<ElementRef<HTMLElement>>;

  private readonly dashboardService = inject(DashboardService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly datePipe = inject(DatePipe);

  private readonly tooltipInstances = new Map<HTMLElement, Tooltip>();
  private needsTooltipRefresh = false;

  readonly loading = signal(false);
  readonly loadingCalendar = signal(false);
  readonly data = signal<DashboardData | null>(null);

  readonly calendarYear = signal(new Date().getFullYear());
  readonly calendarMonth = signal(new Date().getMonth());
  readonly calendarRows = signal<DashboardCalendarRow[]>([]);

  readonly weekDayLabels = computed(() => {
    const locale = this.calendarLocale();
    const sunday = new Date(2026, 0, 4);

    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(sunday);
      date.setDate(sunday.getDate() + index);

      return date.toLocaleDateString(locale, {
        weekday: 'short'
      });
    });
  });

  private readonly languageChangeEffect = effect(() => {
    this.languageService.currentLanguage();
    this.needsTooltipRefresh = true;
  });

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

    return date.toLocaleDateString(this.calendarLocale(), {
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

    this.dashboardService.loadCalendarData(
      this.calendarRangeStart(),
      this.calendarRangeEnd()
    ).subscribe({
      next: (calendarData) => {
        this.calendarRows.set(this.buildCalendarRows(calendarData));
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
    if (segment.startsAtCheckIn && segment.endsAtCheckOut) {
      return 'calendar-event-single';
    }

    if (segment.startsAtCheckIn) {
      return 'calendar-event-start';
    }

    if (segment.endsAtCheckOut) {
      return 'calendar-event-end';
    }

    return 'calendar-event-middle';
  }

  scheduledMaintenanceSegmentClass(segment: DashboardScheduledMaintenanceCalendarSegment): string {
    switch (segment.status) {
      case 'ACTIVE':
        return 'calendar-event-scheduled-active';
      case 'PAUSED':
        return 'calendar-event-scheduled-paused';
      case 'COMPLETED':
        return 'calendar-event-scheduled-completed';
      case 'DELETED':
        return 'calendar-event-scheduled-deleted';
      default:
        return 'calendar-event-scheduled-default';
    }
  }

  scheduledMaintenanceSegmentShapeClass(segment: DashboardScheduledMaintenanceCalendarSegment): string {
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

  scheduledMaintenanceSegmentGridColumn(segment: DashboardScheduledMaintenanceCalendarSegment): string {
    return `${segment.gridColumnStart} / ${segment.gridColumnEnd}`;
  }

  scheduledMaintenanceSegmentGridRow(segment: DashboardScheduledMaintenanceCalendarSegment): string {
    return `${segment.lane + 1}`;
  }

  calendarIconClass(icon: DashboardCalendarDayIcon): string {
    switch (icon.type) {
      case 'MAINTENANCE_RECORD':
        return 'calendar-day-icon-maintenance';
      case 'TASK_LIST':
        return 'calendar-day-icon-task';
      case 'PURCHASE_LIST':
        return 'calendar-day-icon-purchase';
      default:
        return 'calendar-day-icon-default';
    }
  }

  calendarIconBootstrapIcon(icon: DashboardCalendarDayIcon): string {
    switch (icon.type) {
      case 'MAINTENANCE_RECORD':
        return 'bi-tools';
      case 'TASK_LIST':
        return 'bi-check2-square';
      case 'PURCHASE_LIST':
        return 'bi-cart-check';
      default:
        return 'bi-dot';
    }
  }

  segmentMarginLeft(segment: DashboardReservationCalendarSegment): string {
    if (!segment.startsAtCheckIn) {
      return '0';
    }

    if (segment.startsAtCheckIn && segment.endsAtCheckOut && this.calendarSegmentColumnSpan(segment) === 1) {
      return '25%';
    }

    return `calc(100% / ${this.calendarSegmentColumnSpan(segment)} / 2)`;
  }

  segmentMarginRight(segment: DashboardReservationCalendarSegment): string {
    if (!segment.endsAtCheckOut) {
      return '0';
    }

    if (segment.startsAtCheckIn && segment.endsAtCheckOut && this.calendarSegmentColumnSpan(segment) === 1) {
      return '25%';
    }

    return `calc(100% / ${this.calendarSegmentColumnSpan(segment)} / 2)`;
  }

  calendarSegmentGridColumn(segment: DashboardReservationCalendarSegment): string {
    return `${segment.gridColumnStart} / ${segment.gridColumnEnd}`;
  }

  calendarSegmentGridRow(segment: DashboardReservationCalendarSegment): string {
    return `${segment.lane + 1}`;
  }

  calendarRowMinHeightRem(row: DashboardCalendarRow): number {
    return 4.8 + Math.max(1, row.maxLanes) * 1.7 + Math.max(1, row.maxIconRows) * 1.25;
  }

  invoiceStatusLabel(invoiceStatus: 'INVOICED' | 'NOT_INVOICED'): string {
    return invoiceStatus === 'INVOICED'
      ? this.languageService.instant('dashboard.calendar.invoiced')
      : this.languageService.instant('dashboard.calendar.notInvoiced');
  }

  calendarTooltipHtml(segment: DashboardReservationCalendarSegment): string {
    this.languageService.currentLanguage();

    const coverImageHtml = segment.propertyCoverImageUrl
      ? `<img class="reservation-calendar-tooltip-cover" src="${this.escapeHtml(segment.propertyCoverImageUrl)}" alt="${this.escapeHtml(segment.propertyName)}">`
      : `<div class="reservation-calendar-tooltip-cover reservation-calendar-tooltip-cover-placeholder"><span>🏠</span></div>`;

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
      <div class="reservation-calendar-tooltip-content reservation-calendar-tooltip-content-with-cover">
        <div class="reservation-calendar-tooltip-header">
          ${coverImageHtml}
          <div>
            <div class="reservation-calendar-tooltip-title">${this.escapeHtml(segment.primaryGuestName)}</div>
            <div class="reservation-calendar-tooltip-property-top">${this.escapeHtml(segment.propertyName)}</div>
          </div>
        </div>
        ${rowsHtml}
      </div>
    `;
  }

  scheduledMaintenanceTooltipHtml(segment: DashboardScheduledMaintenanceCalendarSegment): string {
    this.languageService.currentLanguage();

    return this.calendarTooltipContentHtml(
      segment.title,
      [
        [this.languageService.instant('dashboard.calendar.tooltip.type'), segment.maintenanceTypeName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.category'), segment.maintenanceCategoryName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.person'), segment.maintenancePersonName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.property'), segment.propertyName],
        [this.languageService.instant('dashboard.calendar.tooltip.cost'), this.formatMoney(segment.estimatedCost)],
        [this.languageService.instant('dashboard.calendar.tooltip.nextDueDate'), segment.nextDueDate || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.status'), this.languageService.instant(`scheduledMaintenance.status.${segment.status}`)]
      ]
    );
  }

  calendarIconTooltipHtml(icon: DashboardCalendarDayIcon): string {
    this.languageService.currentLanguage();

    switch (icon.type) {
      case 'MAINTENANCE_RECORD':
        return this.maintenanceRecordIconTooltipHtml(icon);
      case 'TASK_LIST':
        return this.taskListIconTooltipHtml(icon);
      case 'PURCHASE_LIST':
        return this.purchaseListIconTooltipHtml(icon);
      default:
        return this.calendarTooltipContentHtml(icon.title, []);
    }
  }

  private maintenanceRecordIconTooltipHtml(icon: DashboardCalendarDayIcon): string {
    const maintenance = icon.maintenanceRecord;

    if (!maintenance) {
      return this.calendarTooltipContentHtml(icon.title, []);
    }

    return this.calendarTooltipContentHtml(
      maintenance.title,
      [
        [this.languageService.instant('dashboard.calendar.tooltip.type'), maintenance.maintenanceTypeName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.category'), maintenance.maintenanceCategoryName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.person'), maintenance.maintenancePersonName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.property'), maintenance.propertyName],
        [this.languageService.instant('dashboard.calendar.tooltip.materialsTotal'), String(maintenance.materialsTotal)],
        [this.languageService.instant('dashboard.calendar.tooltip.peopleTotal'), String(maintenance.peopleTotal)],
        [this.languageService.instant('dashboard.calendar.tooltip.status'), this.languageService.instant(`maintenance.status.${maintenance.status}`)],
        [this.languageService.instant('dashboard.calendar.tooltip.cost'), this.formatMoney(maintenance.cost)],
        [this.languageService.instant('dashboard.calendar.tooltip.performedAt'), maintenance.performedAt || '—']
      ]
    );
  }

  private taskListIconTooltipHtml(icon: DashboardCalendarDayIcon): string {
    const taskList = icon.taskList;

    if (!taskList) {
      return this.calendarTooltipContentHtml(icon.title, []);
    }

    return this.calendarTooltipContentHtml(
      taskList.title,
      [
        [this.languageService.instant('dashboard.calendar.tooltip.property'), taskList.propertyName],
        [this.languageService.instant('dashboard.calendar.tooltip.progress'), this.progressLabel(taskList.completedItems, taskList.totalItems)],
        [this.languageService.instant('dashboard.calendar.tooltip.status'), this.languageService.instant(`tasks.status.${taskList.status}`)],
        [this.languageService.instant('dashboard.calendar.tooltip.creationDate'), this.formatDateTime(taskList.creationDate || taskList.createdAt)],
        [this.languageService.instant('dashboard.calendar.tooltip.reservation'), taskList.reservationLabel || this.shortId(taskList.reservationId) || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.maintenance'), taskList.maintenanceRecordLabel || this.shortId(taskList.maintenanceRecordId) || '—']
      ]
    );
  }

  private purchaseListIconTooltipHtml(icon: DashboardCalendarDayIcon): string {
    const purchaseList = icon.purchaseList;

    if (!purchaseList) {
      return this.calendarTooltipContentHtml(icon.title, []);
    }

    return this.calendarTooltipContentHtml(
      purchaseList.cityName || this.languageService.instant('dashboard.calendar.purchase'),
      [
        [this.languageService.instant('dashboard.calendar.tooltip.city'), purchaseList.cityName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.supplier'), purchaseList.supplierName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.property'), purchaseList.propertyName || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.progress'), this.progressLabel(purchaseList.purchasedItems, purchaseList.totalItems)],
        [this.languageService.instant('dashboard.calendar.tooltip.status'), this.languageService.instant(`purchases.status.${purchaseList.status}`)],
        [this.languageService.instant('dashboard.calendar.tooltip.creationDate'), this.formatDateTime(purchaseList.createdAt) || '—'],
        [this.languageService.instant('dashboard.calendar.tooltip.estimatedTotal'), this.formatMoney(purchaseList.estimatedTotal)]
      ]
    );
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

  private buildCalendarRows(calendarData: DashboardCalendarData): DashboardCalendarRow[] {
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
          today: dateString === today,
          icons: []
        });
      }

      this.assignDayIcons(days, calendarData);

      const reservationSegments = this.segmentsForCalendarRow(days, calendarData.reservations);
      const reservationLaneCount = reservationSegments.length === 0
        ? 0
        : Math.max(...reservationSegments.map((segment) => segment.lane)) + 1;
      const scheduledMaintenanceSegments = this.scheduledMaintenanceSegmentsForCalendarRow(
        days,
        calendarData.scheduledMaintenances,
        reservationLaneCount
      );
      const scheduledLaneCount = scheduledMaintenanceSegments.length === 0
        ? 0
        : Math.max(...scheduledMaintenanceSegments.map((segment) => segment.lane - reservationLaneCount)) + 1;
      const maxIconRows = Math.max(1, ...days.map((day) => day.icons.length));

      rows.push({
        id: days[0].date,
        days,
        segments: reservationSegments,
        scheduledMaintenanceSegments,
        maxLanes: Math.max(1, reservationLaneCount + scheduledLaneCount),
        maxIconRows
      });
    }

    return rows;
  }

  private assignDayIcons(days: DashboardCalendarDay[], calendarData: DashboardCalendarData): void {
    const daysByDate = new Map(days.map((day) => [day.date, day]));

    for (const maintenance of calendarData.maintenanceRecords) {
      const date = this.normalizeDashboardDate(maintenance.scheduledAt);

      if (!date) {
        continue;
      }

      daysByDate.get(date)?.icons.push({
        id: `maintenance-${maintenance.id}`,
        type: 'MAINTENANCE_RECORD',
        date,
        title: maintenance.title,
        status: maintenance.status,
        maintenanceRecord: maintenance
      });
    }

    for (const taskList of calendarData.taskLists) {
      const date = this.normalizeDashboardDate(taskList.dueDate);

      if (!date) {
        continue;
      }

      daysByDate.get(date)?.icons.push({
        id: `task-${taskList.id}`,
        type: 'TASK_LIST',
        date,
        title: taskList.title,
        status: taskList.status,
        taskList
      });
    }

    for (const purchaseList of calendarData.purchaseLists) {
      const date = this.normalizeDashboardDate(purchaseList.purchaseDate);

      if (!date) {
        continue;
      }

      daysByDate.get(date)?.icons.push({
        id: `purchase-${purchaseList.id}`,
        type: 'PURCHASE_LIST',
        date,
        title: purchaseList.supplierName || purchaseList.cityName || this.languageService.instant('dashboard.calendar.purchase'),
        status: purchaseList.status,
        purchaseList
      });
    }

    for (const day of days) {
      day.icons.sort((left, right) => {
        const order = {
          MAINTENANCE_RECORD: 1,
          TASK_LIST: 2,
          PURCHASE_LIST: 3
        };

        return order[left.type] - order[right.type] || left.title.localeCompare(right.title);
      });
    }
  }

  private segmentsForCalendarRow(days: DashboardCalendarDay[], reservations: DashboardReservationDetail[]): DashboardReservationCalendarSegment[] {
    const rowStartDate = this.parseDashboardDate(days[0].date);
    const rowEndDate = this.parseDashboardDate(days[6].date);

    if (!rowStartDate || !rowEndDate) {
      return [];
    }

    const overlappingReservations: Array<{
      reservation: DashboardReservationDetail;
      checkInDate: Date;
      checkOutDate: Date;
      segmentStartDate: Date;
      segmentEndDate: Date;
      startIndex: number;
      endIndex: number;
    }> = [];

    for (const reservation of reservations) {
      const checkInDate = this.parseDashboardDate(reservation.checkIn);
      const checkOutDate = this.parseDashboardDate(reservation.checkOut);

      if (!checkInDate || !checkOutDate) {
        continue;
      }

      if (this.compareDashboardDates(checkOutDate, rowStartDate) < 0 || this.compareDashboardDates(checkInDate, rowEndDate) > 0) {
        continue;
      }

      const segmentStartDate = this.maxDashboardDate(checkInDate, rowStartDate);
      const segmentEndDate = this.minDashboardDate(checkOutDate, rowEndDate);
      const startIndex = this.daysBetween(rowStartDate, segmentStartDate);
      const endIndex = this.daysBetween(rowStartDate, segmentEndDate);

      if (startIndex < 0 || startIndex > 6 || endIndex < 0 || endIndex > 6 || startIndex > endIndex) {
        continue;
      }

      overlappingReservations.push({
        reservation,
        checkInDate,
        checkOutDate,
        segmentStartDate,
        segmentEndDate,
        startIndex,
        endIndex
      });
    }

    overlappingReservations.sort((left, right) => {
      const byStart = this.compareDashboardDates(left.segmentStartDate, right.segmentStartDate);

      if (byStart !== 0) {
        return byStart;
      }

      return this.compareDashboardDates(right.segmentEndDate, left.segmentEndDate);
    });

    const laneEndIndexes: number[] = [];
    const segments: DashboardReservationCalendarSegment[] = [];

    for (const item of overlappingReservations) {
      const lane = this.findAvailableLane(laneEndIndexes, item.startIndex);
      laneEndIndexes[lane] = item.endIndex;

      segments.push(this.toCalendarSegment(
        item.reservation,
        item.checkInDate,
        item.checkOutDate,
        item.segmentStartDate,
        item.segmentEndDate,
        item.startIndex,
        item.endIndex,
        lane
      ));
    }

    return segments;
  }

  private scheduledMaintenanceSegmentsForCalendarRow(
    days: DashboardCalendarDay[],
    scheduledMaintenances: DashboardScheduledMaintenanceCalendarItem[],
    laneOffset: number
  ): DashboardScheduledMaintenanceCalendarSegment[] {
    const rowStartDate = this.parseDashboardDate(days[0].date);
    const rowEndDate = this.parseDashboardDate(days[6].date);

    if (!rowStartDate || !rowEndDate) {
      return [];
    }

    const overlappingItems: Array<{
      maintenance: DashboardScheduledMaintenanceCalendarItem;
      startDate: Date;
      endDate: Date;
      segmentStartDate: Date;
      segmentEndDate: Date;
      startIndex: number;
      endIndex: number;
    }> = [];

    for (const maintenance of scheduledMaintenances) {
      const startDate = this.parseDashboardDate(maintenance.startDate);
      const endDate = this.parseDashboardDate(maintenance.endDate ?? maintenance.startDate);

      if (!startDate || !endDate) {
        continue;
      }

      if (this.compareDashboardDates(endDate, rowStartDate) < 0 || this.compareDashboardDates(startDate, rowEndDate) > 0) {
        continue;
      }

      const segmentStartDate = this.maxDashboardDate(startDate, rowStartDate);
      const segmentEndDate = this.minDashboardDate(endDate, rowEndDate);
      const startIndex = this.daysBetween(rowStartDate, segmentStartDate);
      const endIndex = this.daysBetween(rowStartDate, segmentEndDate);

      if (startIndex < 0 || startIndex > 6 || endIndex < 0 || endIndex > 6 || startIndex > endIndex) {
        continue;
      }

      overlappingItems.push({
        maintenance,
        startDate,
        endDate,
        segmentStartDate,
        segmentEndDate,
        startIndex,
        endIndex
      });
    }

    overlappingItems.sort((left, right) => {
      const byStart = this.compareDashboardDates(left.segmentStartDate, right.segmentStartDate);

      if (byStart !== 0) {
        return byStart;
      }

      return this.compareDashboardDates(right.segmentEndDate, left.segmentEndDate);
    });

    const laneEndIndexes: number[] = [];
    const segments: DashboardScheduledMaintenanceCalendarSegment[] = [];

    for (const item of overlappingItems) {
      const lane = this.findAvailableLane(laneEndIndexes, item.startIndex);
      laneEndIndexes[lane] = item.endIndex;

      segments.push(this.toScheduledMaintenanceSegment(
        item.maintenance,
        item.startDate,
        item.endDate,
        item.segmentStartDate,
        item.segmentEndDate,
        item.startIndex,
        item.endIndex,
        lane + laneOffset
      ));
    }

    return segments;
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
    checkInDate: Date,
    checkOutDate: Date,
    segmentStartDate: Date,
    segmentEndDate: Date,
    startIndex: number,
    endIndex: number,
    lane: number
  ): DashboardReservationCalendarSegment {
    const guests = reservation.guests ?? [];
    const primaryGuest = guests.find((guest) => guest.primary) ?? guests[0] ?? null;
    const invoiceStatus = reservation.invoiceNumber || reservation.invoiceSeries ? 'INVOICED' : 'NOT_INVOICED';
    const checkIn = this.toLocalDateString(checkInDate);
    const checkOut = this.toLocalDateString(checkOutDate);
    const segmentStart = this.toLocalDateString(segmentStartDate);
    const segmentEnd = this.toLocalDateString(segmentEndDate);

    return {
      id: `${reservation.id}-${segmentStart}-${segmentEnd}`,
      reservationId: reservation.id,
      propertyId: reservation.propertyId,
      propertyName: reservation.propertyName,
      propertyCoverImageUrl: reservation.propertyCoverImageUrl ?? null,
      platformName: reservation.platformName,
      reservationCode: reservation.reservationCode,
      checkIn,
      checkOut,
      guestNames: guests.map((guest) => guest.fullName).filter((name): name is string => !!name),
      primaryGuestName: primaryGuest?.fullName ?? this.languageService.instant('dashboard.calendar.noGuest'),
      guestCount: guests.length,
      invoiceStatus,
      status: reservation.status,
      rangeStartsHere: this.isSameDashboardDate(checkInDate, segmentStartDate),
      rangeEndsHere: this.isSameDashboardDate(checkOutDate, segmentEndDate),
      startsAtCheckIn: this.isSameDashboardDate(checkInDate, segmentStartDate),
      endsAtCheckOut: this.isSameDashboardDate(checkOutDate, segmentEndDate),
      gridColumnStart: startIndex + 1,
      gridColumnEnd: endIndex + 2,
      lane,
      topRem: 2.35 + lane * 1.55
    };
  }

  private toScheduledMaintenanceSegment(
    maintenance: DashboardScheduledMaintenanceCalendarItem,
    startDate: Date,
    endDate: Date,
    segmentStartDate: Date,
    segmentEndDate: Date,
    startIndex: number,
    endIndex: number,
    lane: number
  ): DashboardScheduledMaintenanceCalendarSegment {
    const startDateString = this.toLocalDateString(startDate);
    const endDateString = this.toLocalDateString(endDate);
    const segmentStart = this.toLocalDateString(segmentStartDate);
    const segmentEnd = this.toLocalDateString(segmentEndDate);

    return {
      id: `${maintenance.id}-${segmentStart}-${segmentEnd}`,
      scheduledMaintenanceId: maintenance.id,
      propertyId: maintenance.propertyId,
      propertyName: maintenance.propertyName,
      maintenanceCategoryName: maintenance.maintenanceCategoryName,
      maintenanceTypeName: maintenance.maintenanceTypeName,
      maintenancePersonName: maintenance.maintenancePersonName,
      title: maintenance.title,
      startDate: startDateString,
      endDate: endDateString,
      nextDueDate: maintenance.nextDueDate,
      estimatedCost: maintenance.estimatedCost,
      status: maintenance.status,
      rangeStartsHere: this.isSameDashboardDate(startDate, segmentStartDate),
      rangeEndsHere: this.isSameDashboardDate(endDate, segmentEndDate),
      gridColumnStart: startIndex + 1,
      gridColumnEnd: endIndex + 2,
      lane
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

  private toLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  private refreshBootstrapTooltips(): void {
    this.disposeTooltips();

    const elements = this.calendarTooltipElements?.toArray() ?? [];

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

  private calendarTooltipContentHtml(title: string, rows: Array<[string, string]>): string {
    const rowsHtml = rows.map(([label, value]) => `
      <div class="reservation-calendar-tooltip-row">
        <span>${this.escapeHtml(label)}</span>
        <strong>${this.escapeHtml(value)}</strong>
      </div>
    `).join('');

    return `
      <div class="reservation-calendar-tooltip-content">
        <div class="reservation-calendar-tooltip-title">${this.escapeHtml(title)}</div>
        ${rowsHtml}
      </div>
    `;
  }

  private formatMoney(value: number | null | undefined): string {
    if (value === null || value === undefined) {
      return '—';
    }

    return new Intl.NumberFormat(this.calendarLocale(), {
      style: 'currency',
      currency: 'GTQ'
    }).format(value);
  }

  private progressLabel(completedItems: number, totalItems: number): string {
    if (!totalItems) {
      return '0/0 (0%)';
    }

    const percentage = Math.round((completedItems / totalItems) * 100);

    return `${completedItems}/${totalItems} (${percentage}%)`;
  }

  private shortId(value: string | null | undefined): string | null {
    return value ? value.slice(0, 8) : null;
  }

  private normalizeDashboardDate(value: string | null | undefined): string | null {
    if (!value) {
      return null;
    }

    return String(value).slice(0, 10);
  }

  private calendarSegmentColumnSpan(segment: DashboardReservationCalendarSegment): number {
    return Math.max(1, segment.gridColumnEnd - segment.gridColumnStart);
  }

  private calendarLocale(): string {
    return this.languageService.currentLanguage() === 'es' ? 'es-GT' : 'en-US';
  }

  private parseDashboardDate(value: string): Date | null {
    const rawValue = String(value ?? '').trim();

    if (!rawValue) {
      return null;
    }

    const isoMatch = rawValue.match(/^(\d{4})-(\d{2})-(\d{2})/);

    if (isoMatch) {
      return new Date(Number(isoMatch[1]), Number(isoMatch[2]) - 1, Number(isoMatch[3]));
    }

    const fallbackDate = new Date(rawValue);

    if (Number.isNaN(fallbackDate.getTime())) {
      return null;
    }

    return new Date(fallbackDate.getFullYear(), fallbackDate.getMonth(), fallbackDate.getDate());
  }

  private compareDashboardDates(left: Date, right: Date): number {
    return this.dashboardDateNumber(left) - this.dashboardDateNumber(right);
  }

  private isSameDashboardDate(left: Date, right: Date): boolean {
    return this.compareDashboardDates(left, right) === 0;
  }

  private maxDashboardDate(left: Date, right: Date): Date {
    return this.compareDashboardDates(left, right) >= 0 ? left : right;
  }

  private minDashboardDate(left: Date, right: Date): Date {
    return this.compareDashboardDates(left, right) <= 0 ? left : right;
  }

  private daysBetween(start: Date, end: Date): number {
    return this.dashboardDateNumber(end) - this.dashboardDateNumber(start);
  }

  private dashboardDateNumber(date: Date): number {
    return Math.floor(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86_400_000);
  }

  private formatDateTime(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }

    return this.datePipe.transform(value, 'dd/MM/yyyy HH:mm:ss') ?? value;
  }
}
