import { DecimalPipe, NgClass } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../../../core/models/api-error.model';
import { LanguageService } from '../../../../core/i18n/language.service';
import { QuetzalCurrencyPipe } from '../../../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../../../shared/toast/toast.service';
import { DashboardAnalyticsResponse, TopItemResponse } from '../../models/dashboard-analytics.model';
import { DashboardAnalyticsService } from '../../services/dashboard-analytics.service';

@Component({
  selector: 'app-dashboard-analytics',
  standalone: true,
  imports: [DecimalPipe, NgClass, TranslatePipe, QuetzalCurrencyPipe],
  templateUrl: './dashboard-analytics.component.html'
})
export class DashboardAnalyticsComponent implements OnInit {
  private readonly dashboardAnalyticsService = inject(DashboardAnalyticsService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal(false);
  readonly analytics = signal<DashboardAnalyticsResponse | null>(null);

  readonly maxMaintenanceCost = computed(() => this.maxAmount(this.analytics()?.maintenanceCostByMonth ?? []));
  readonly maxPurchaseCost = computed(() => this.maxAmount(this.analytics()?.purchaseCostByMonth ?? []));
  readonly maxReservationsCount = computed(() => {
    const rows = this.analytics()?.reservationsByMonth ?? [];
    return rows.reduce((max, row) => Math.max(max, row.count ?? 0), 0);
  });
  readonly maxReservationSupplyQuantity = computed(() => this.maxQuantity(this.analytics()?.topReservationSupplies ?? []));
  readonly maxPurchasedItemQuantity = computed(() => this.maxQuantity(this.analytics()?.topPurchasedItems ?? []));

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.loading.set(true);

    this.dashboardAnalyticsService.loadAnalytics().subscribe({
      next: (analytics) => {
        this.analytics.set(analytics);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('dashboard.analytics.messages.loadError')));
      }
    });
  }

  barWidth(value: number | null | undefined, max: number): string {
    if (!value || max <= 0) {
      return '0%';
    }

    return `${Math.max(6, Math.round((value / max) * 100))}%`;
  }

  trackByMonth(_index: number, row: { month: string }): string {
    return row.month;
  }

  trackByName(_index: number, row: TopItemResponse): string {
    return row.name;
  }

  private maxAmount(rows: Array<{ amount: number | null }>): number {
    return rows.reduce((max, row) => Math.max(max, row.amount ?? 0), 0);
  }

  private maxQuantity(rows: Array<{ quantity: number | null }>): number {
    return rows.reduce((max, row) => Math.max(max, row.quantity ?? 0), 0);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const apiError = error as ApiError;

    if (typeof apiError?.error === 'string' && apiError.error.trim()) {
      return apiError.error;
    }

    if (typeof apiError?.message === 'string' && apiError.message.trim()) {
      return apiError.message;
    }

    return fallback;
  }
}
