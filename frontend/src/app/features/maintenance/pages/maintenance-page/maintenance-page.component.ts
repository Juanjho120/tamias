import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceImagesModalComponent } from '../../components/maintenance-images-modal/maintenance-images-modal.component';
import { MaintenanceRecordSummary, MaintenanceStatus, MAINTENANCE_STATUSES } from '../../models/maintenance-record.model';
import { MaintenanceRecordService } from '../../services/maintenance-record.service';

@Component({
  selector: 'app-maintenance-page',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule, NgClass, TranslatePipe, MaintenanceImagesModalComponent],
  templateUrl: './maintenance-page.component.html'
})
export class MaintenancePageComponent implements OnInit {
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = MAINTENANCE_STATUSES;

  readonly loading = signal(false);
  readonly records = signal<MaintenanceRecordSummary[]>([]);
  readonly selectedRecordForImages = signal<MaintenanceRecordSummary | null>(null);

  readonly status = signal<MaintenanceStatus | ''>('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('maintenance.pagination.noRecords');
    }

    return this.languageService.instant('maintenance.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  constructor(private readonly maintenanceRecordService: MaintenanceRecordService) {
  }

  ngOnInit(): void {
    this.loadRecords();
  }

  loadRecords(): void {
    this.loading.set(true);

    this.maintenanceRecordService.findAll({
      status: this.status(),
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<MaintenanceRecordSummary>) => {
        this.records.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadRecords();
  }

  clearFilters(): void {
    this.status.set('');
    this.page.set(0);
    this.loadRecords();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadRecords();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadRecords();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadRecords();
  }

  openImages(record: MaintenanceRecordSummary): void {
    this.selectedRecordForImages.set(record);
  }

  closeImages(): void {
    this.selectedRecordForImages.set(null);
  }

  statusBadgeClass(status: MaintenanceStatus): string {
    switch (status) {
      case 'PENDING':
        return 'text-bg-warning';
      case 'IN_PROGRESS':
        return 'text-bg-info';
      case 'COMPLETED':
        return 'text-bg-success';
      case 'CANCELLED':
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
