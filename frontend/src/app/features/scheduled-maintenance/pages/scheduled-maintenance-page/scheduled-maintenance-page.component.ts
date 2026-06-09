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
import { ScheduledMaintenanceActionModalComponent, ScheduledMaintenanceActionType } from '../../components/scheduled-maintenance-action-modal/scheduled-maintenance-action-modal.component';
import { ScheduledMaintenanceFormModalComponent } from '../../components/scheduled-maintenance-form-modal/scheduled-maintenance-form-modal.component';
import { ScheduledMaintenanceHistoryModalComponent } from '../../components/scheduled-maintenance-history-modal/scheduled-maintenance-history-modal.component';
import {
  ScheduledMaintenance,
  ScheduledMaintenanceHistory,
  ScheduledMaintenanceRequest,
  ScheduledMaintenanceStatus,
  ScheduledMaintenanceSummary,
  SCHEDULED_MAINTENANCE_STATUSES
} from '../../models/scheduled-maintenance.model';
import { ScheduledMaintenanceReferenceData, ScheduledMaintenanceReferenceDataService } from '../../services/scheduled-maintenance-reference-data.service';
import { ScheduledMaintenanceService } from '../../services/scheduled-maintenance.service';

type FormMode = 'create' | 'edit';
type ViewMode = 'all' | 'due';

@Component({
  selector: 'app-scheduled-maintenance-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    QuetzalCurrencyPipe,
    ScheduledMaintenanceActionModalComponent,
    ScheduledMaintenanceFormModalComponent,
    ScheduledMaintenanceHistoryModalComponent
  ],
  templateUrl: './scheduled-maintenance-page.component.html'
})
export class ScheduledMaintenancePageComponent implements OnInit {
  private readonly scheduledMaintenanceService = inject(ScheduledMaintenanceService);
  private readonly referenceDataService = inject(ScheduledMaintenanceReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = SCHEDULED_MAINTENANCE_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly actionLoading = signal(false);
  readonly loadingReferences = signal(false);
  readonly historyLoading = signal(false);

  readonly items = signal<ScheduledMaintenanceSummary[]>([]);
  readonly selectedItem = signal<ScheduledMaintenance | null>(null);
  readonly itemToDelete = signal<ScheduledMaintenanceSummary | null>(null);
  readonly itemForAction = signal<ScheduledMaintenanceSummary | null>(null);
  readonly action = signal<ScheduledMaintenanceActionType | null>(null);
  readonly historyTitle = signal('');
  readonly historyItems = signal<ScheduledMaintenanceHistory[]>([]);
  readonly historyVisible = signal(false);

  readonly references = signal<ScheduledMaintenanceReferenceData>({
    properties: [],
    categories: [],
    types: [],
    people: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');
  readonly viewMode = signal<ViewMode>('all');

  readonly propertyId = signal('');
  readonly status = signal<ScheduledMaintenanceStatus | ''>('');
  readonly dueDate = signal(this.today());
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('scheduledMaintenance.pagination.noItems');
    }

    return this.languageService.instant('scheduledMaintenance.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const item = this.itemToDelete();

    if (!item) {
      return '';
    }

    return this.languageService.instant('scheduledMaintenance.confirmDeleteMessage', {
      title: item.title
    });
  });

  readonly actionTitle = computed(() => this.itemForAction()?.title ?? '');

  ngOnInit(): void {
    this.loadReferences();
    this.loadItems();
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.referencesError')));
      }
    });
  }

  loadItems(): void {
    this.loading.set(true);

    const request = this.viewMode() === 'due'
      ? this.scheduledMaintenanceService.findDue(this.dueDate(), this.page(), this.size())
      : this.scheduledMaintenanceService.findAll({
        propertyId: this.propertyId() || undefined,
        status: this.status(),
        page: this.page(),
        size: this.size(),
        sort: 'nextDueDate,asc'
      });

    request.subscribe({
      next: (response: PageResponse<ScheduledMaintenanceSummary>) => {
        this.items.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.loadError')));
      }
    });
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode.set(mode);
    this.page.set(0);
    this.loadItems();
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadItems();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.status.set('');
    this.dueDate.set(this.today());
    this.page.set(0);
    this.loadItems();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadItems();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadItems();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadItems();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedItem.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.scheduledMaintenanceService.findById(id).subscribe({
      next: (item: ScheduledMaintenance) => {
        this.selectedItem.set(item);
        this.formMode.set('edit');
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedItem.set(null);
    this.formMode.set('create');
  }

  saveItem(request: ScheduledMaintenanceRequest): void {
    const selectedItem = this.selectedItem();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedItem
      ? this.scheduledMaintenanceService.update(selectedItem.id, request)
      : this.scheduledMaintenanceService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('scheduledMaintenance.messages.updated')
            : this.languageService.instant('scheduledMaintenance.messages.created')
        );
        this.closeForm();
        this.loadItems();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.saveError')));
      }
    });
  }

  requestDelete(item: ScheduledMaintenanceSummary): void {
    this.itemToDelete.set(item);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.itemToDelete.set(null);
  }

  confirmDelete(): void {
    const item = this.itemToDelete();

    if (!item) {
      return;
    }

    this.deletingId.set(item.id);

    this.scheduledMaintenanceService.delete(item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.itemToDelete.set(null);
        this.toastService.success(this.languageService.instant('scheduledMaintenance.messages.deleted'));
        this.loadItems();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.deleteError')));
      }
    });
  }

  openAction(item: ScheduledMaintenanceSummary, action: ScheduledMaintenanceActionType): void {
    this.itemForAction.set(item);
    this.action.set(action);
  }

  closeAction(): void {
    if (this.actionLoading()) {
      return;
    }

    this.itemForAction.set(null);
    this.action.set(null);
  }

  confirmAction(payload: { nextDueDate: string | null; reason: string | null }): void {
    const item = this.itemForAction();
    const action = this.action();

    if (!item || !action) {
      return;
    }

    this.actionLoading.set(true);

    let request;

    if (action === 'reschedule') {
      request = this.scheduledMaintenanceService.reschedule(item.id, {
        nextDueDate: payload.nextDueDate ?? '',
        reason: payload.reason
      });
    } else if (action === 'pause') {
      request = this.scheduledMaintenanceService.pause(item.id, { reason: payload.reason });
    } else if (action === 'resume') {
      request = this.scheduledMaintenanceService.resume(item.id, { reason: payload.reason });
    } else {
      request = this.scheduledMaintenanceService.cancel(item.id, { reason: payload.reason });
    }

    request.subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.toastService.success(this.languageService.instant('scheduledMaintenance.messages.actionSuccess'));
        this.closeAction();
        this.loadItems();
      },
      error: (error: unknown) => {
        this.actionLoading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.actionError')));
      }
    });
  }

  generateRecord(item: ScheduledMaintenanceSummary): void {
    this.actionLoading.set(true);

    this.scheduledMaintenanceService.generateMaintenanceRecord(item.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.toastService.success(this.languageService.instant('scheduledMaintenance.messages.recordGenerated'));
        this.loadItems();
      },
      error: (error: unknown) => {
        this.actionLoading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.generateError')));
      }
    });
  }

  openHistory(item: ScheduledMaintenanceSummary): void {
    this.historyVisible.set(true);
    this.historyTitle.set(item.title);
    this.historyLoading.set(true);
    this.historyItems.set([]);

    this.scheduledMaintenanceService.findHistory(item.id).subscribe({
      next: (history) => {
        this.historyItems.set(history);
        this.historyLoading.set(false);
      },
      error: (error: unknown) => {
        this.historyLoading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('scheduledMaintenance.messages.historyError')));
      }
    });
  }

  closeHistory(): void {
    if (this.historyLoading()) {
      return;
    }

    this.historyVisible.set(false);
    this.historyTitle.set('');
    this.historyItems.set([]);
  }

  statusBadgeClass(status: ScheduledMaintenanceStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'PAUSED':
        return 'text-bg-warning';
      case 'COMPLETED':
        return 'text-bg-info';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  frequencyLabelKey(frequency: string): string {
    return `scheduledMaintenance.frequency.${frequency}`;
  }

  canPause(item: ScheduledMaintenanceSummary): boolean {
    return item.status === 'ACTIVE';
  }

  canResume(item: ScheduledMaintenanceSummary): boolean {
    return item.status === 'PAUSED';
  }

  canGenerate(item: ScheduledMaintenanceSummary): boolean {
    return item.status === 'ACTIVE';
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
