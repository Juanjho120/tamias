import { DatePipe, NgClass, PercentPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../core/i18n/language.service';
import { ApiError } from '../../core/models/api-error.model';
import { QuetzalCurrencyPipe } from '../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../shared/toast/toast.service';
import {
  DashboardData,
  DashboardMetric,
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
  readonly data = signal<DashboardData | null>(null);

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

  ngOnInit(): void {
    this.loadDashboard();
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

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
