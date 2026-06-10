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
import { RelatedTaskListsModalComponent } from '../../../tasks/components/related-task-lists-modal/related-task-lists-modal.component';
import { MaintenanceDetailsModalComponent } from '../../components/maintenance-details-modal/maintenance-details-modal.component';
import { MaintenanceImagesModalComponent } from '../../components/maintenance-images-modal/maintenance-images-modal.component';
import { MaintenanceRecordFormModalComponent } from '../../components/maintenance-record-form-modal/maintenance-record-form-modal.component';
import {
  MaintenanceRecord,
  MaintenanceRecordRequest,
  MaintenanceRecordSummary,
  MaintenanceStatus,
  MAINTENANCE_STATUSES
} from '../../models/maintenance-record.model';
import { MaintenanceReferenceData, MaintenanceReferenceDataService } from '../../services/maintenance-reference-data.service';
import { MaintenanceRecordService } from '../../services/maintenance-record.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-maintenance-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    QuetzalCurrencyPipe,
    RelatedTaskListsModalComponent,
    MaintenanceDetailsModalComponent,
    MaintenanceImagesModalComponent,
    MaintenanceRecordFormModalComponent
  ],
  templateUrl: './maintenance-page.component.html'
})
export class MaintenancePageComponent implements OnInit {
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly referenceDataService = inject(MaintenanceReferenceDataService);

  readonly statuses = MAINTENANCE_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);

  readonly records = signal<MaintenanceRecordSummary[]>([]);
  readonly selectedRecord = signal<MaintenanceRecord | null>(null);
  readonly selectedRecordForImages = signal<MaintenanceRecordSummary | null>(null);
  readonly selectedRecordForDetails = signal<MaintenanceRecordSummary | null>(null);
  readonly selectedRecordForTasks = signal<MaintenanceRecordSummary | null>(null);
  readonly recordToDelete = signal<MaintenanceRecordSummary | null>(null);

  readonly references = signal<MaintenanceReferenceData>({
    properties: [],
    categories: [],
    types: [],
    people: [],
    inventoryItems: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly propertyId = signal('');
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

  readonly deleteMessage = computed(() => {
    const record = this.recordToDelete();

    if (!record) {
      return '';
    }

    return this.languageService.instant('maintenance.confirmDeleteMessage', {
      title: record.title
    });
  });

  readonly taskContextTitle = computed(() => {
    const record = this.selectedRecordForTasks();
    return record ? `${record.title} · ${record.propertyName}` : '';
  });

  constructor(private readonly maintenanceRecordService: MaintenanceRecordService) {
  }

  ngOnInit(): void {
    this.loadReferences();
    this.loadRecords();
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.messages.referencesError')));
      }
    });
  }

  loadRecords(): void {
    this.loading.set(true);

    this.maintenanceRecordService.findAll({
      propertyId: this.propertyId() || undefined,
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
    this.propertyId.set('');
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

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedRecord.set(null);
    this.formVisible.set(true);
  }

  openEditForm(recordId: string): void {
    this.loading.set(true);

    this.maintenanceRecordService.findById(recordId).subscribe({
      next: (record: MaintenanceRecord) => {
        this.selectedRecord.set(record);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedRecord.set(null);
    this.formMode.set('create');
  }

  saveRecord(request: MaintenanceRecordRequest): void {
    const selectedRecord = this.selectedRecord();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedRecord
      ? this.maintenanceRecordService.update(selectedRecord.id, request)
      : this.maintenanceRecordService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('maintenance.messages.updated')
            : this.languageService.instant('maintenance.messages.created')
        );
        this.closeForm();
        this.loadRecords();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.messages.saveError')));
      }
    });
  }

  requestDelete(record: MaintenanceRecordSummary): void {
    this.recordToDelete.set(record);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.recordToDelete.set(null);
  }

  confirmDelete(): void {
    const record = this.recordToDelete();

    if (!record) {
      return;
    }

    this.deletingId.set(record.id);

    this.maintenanceRecordService.delete(record.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.recordToDelete.set(null);
        this.toastService.success(this.languageService.instant('maintenance.messages.deleted'));
        this.loadRecords();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.messages.deleteError')));
      }
    });
  }

  openImages(record: MaintenanceRecordSummary): void {
    this.selectedRecordForImages.set(record);
  }

  closeImages(): void {
    this.selectedRecordForImages.set(null);
  }

  openDetails(record: MaintenanceRecordSummary): void {
    this.selectedRecordForDetails.set(record);
  }

  closeDetails(): void {
    this.selectedRecordForDetails.set(null);
  }

  openTasks(record: MaintenanceRecordSummary): void {
    this.selectedRecordForTasks.set(record);
  }

  closeTasks(): void {
    this.selectedRecordForTasks.set(null);
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
