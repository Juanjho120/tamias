import { DatePipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { TaskItemsModalComponent } from '../task-items-modal/task-items-modal.component';
import { TaskListFormModalComponent } from '../task-list-form-modal/task-list-form-modal.component';
import { TaskList, TaskListRequest, TaskListStatus, TaskListSummary } from '../../models/task-list.model';
import { TaskReferenceData, TaskReferenceDataService } from '../../services/task-reference-data.service';
import { TaskListService } from '../../services/task-list.service';

export type RelatedTaskContextType = 'reservation' | 'maintenance';

@Component({
  selector: 'app-related-task-lists-modal',
  standalone: true,
  imports: [
    DatePipe,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    TaskItemsModalComponent,
    TaskListFormModalComponent
  ],
  templateUrl: './related-task-lists-modal.component.html'
})
export class RelatedTaskListsModalComponent implements OnChanges {
  private readonly taskListService = inject(TaskListService);
  private readonly referenceDataService = inject(TaskReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  @Input() open = false;
  @Input() contextType: RelatedTaskContextType = 'reservation';
  @Input() contextId: string | null = null;
  @Input() propertyId: string | null = null;
  @Input() contextTitle = '';

  @Output() close = new EventEmitter<void>();
  @Output() taskListsChanged = new EventEmitter<void>();

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);

  readonly taskLists = signal<TaskListSummary[]>([]);
  readonly selectedTaskList = signal<TaskList | null>(null);
  readonly taskListForItems = signal<TaskListSummary | null>(null);
  readonly taskListToDelete = signal<TaskListSummary | null>(null);

  readonly formVisible = signal(false);

  readonly references = signal<TaskReferenceData>({
    properties: [],
    reservations: [],
    maintenanceRecords: [],
    taskTemplates: []
  });

  readonly titleKey = computed(() => this.contextType === 'reservation'
    ? 'tasks.related.reservationTitle'
    : 'tasks.related.maintenanceTitle'
  );

  readonly defaultTaskTitle = computed(() => this.contextType === 'reservation'
    ? this.languageService.instant('tasks.related.defaultReservationTaskTitle')
    : this.languageService.instant('tasks.related.defaultMaintenanceTaskTitle')
  );

  readonly deleteMessage = computed(() => {
    const taskList = this.taskListToDelete();

    if (!taskList) {
      return '';
    }

    return this.languageService.instant('tasks.confirmDeleteMessage', {
      title: taskList.title
    });
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['contextId'] || changes['contextType']) && this.open && this.contextId) {
      this.loadReferences();
      this.loadTaskLists();
    }
  }

  requestClose(): void {
    if (this.loading() || this.saving() || this.deletingId() || this.loadingReferences()) {
      return;
    }

    this.resetState();
    this.close.emit();
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.referencesError')));
      }
    });
  }

  loadTaskLists(): void {
    if (!this.contextId) {
      return;
    }

    this.loading.set(true);

    this.taskListService.findAll({
      reservationId: this.contextType === 'reservation' ? this.contextId : undefined,
      maintenanceRecordId: this.contextType === 'maintenance' ? this.contextId : undefined,
      page: 0,
      size: 100,
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<TaskListSummary>) => {
        this.taskLists.set(response.content);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.loadError')));
      }
    });
  }

  openCreateForm(): void {
    this.selectedTaskList.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.taskListService.findById(id).subscribe({
      next: (taskList) => {
        this.selectedTaskList.set(taskList);
        this.formVisible.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.detailError')));
      }
    });
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formVisible.set(false);
    this.selectedTaskList.set(null);
  }

  saveTaskList(request: TaskListRequest): void {
    const selectedTaskList = this.selectedTaskList();

    const normalizedRequest: TaskListRequest = {
      ...request,
      propertyId: this.propertyId ?? request.propertyId,
      reservationId: this.contextType === 'reservation' ? this.contextId : null,
      maintenanceRecordId: this.contextType === 'maintenance' ? this.contextId : null
    };

    this.saving.set(true);

    const saveRequest = selectedTaskList
      ? this.taskListService.update(selectedTaskList.id, normalizedRequest)
      : this.taskListService.create(normalizedRequest);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          selectedTaskList
            ? this.languageService.instant('tasks.messages.updated')
            : this.languageService.instant('tasks.messages.created')
        );
        this.closeForm();
        this.taskListsChanged.emit();
        this.loadTaskLists();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.saveError')));
      }
    });
  }

  openItems(taskList: TaskListSummary): void {
    this.taskListForItems.set(taskList);
  }

  closeItems(): void {
    this.taskListForItems.set(null);
  }

  requestDelete(taskList: TaskListSummary): void {
    this.taskListToDelete.set(taskList);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.taskListToDelete.set(null);
  }

  confirmDelete(): void {
    const taskList = this.taskListToDelete();

    if (!taskList) {
      return;
    }

    this.deletingId.set(taskList.id);

    this.taskListService.delete(taskList.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.taskListToDelete.set(null);
        this.toastService.success(this.languageService.instant('tasks.messages.deleted'));
        this.taskListsChanged.emit();
        this.loadTaskLists();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.deleteError')));
      }
    });
  }

  statusBadgeClass(status: TaskListStatus): string {
    switch (status) {
      case 'OPEN':
        return 'text-bg-secondary';
      case 'IN_PROGRESS':
        return 'text-bg-info';
      case 'COMPLETED':
        return 'text-bg-success';
      case 'CANCELLED':
        return 'text-bg-warning';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  completionRatio(taskList: TaskListSummary): number {
    if (!taskList.totalItems) {
      return 0;
    }

    return taskList.completedItems / taskList.totalItems;
  }

  private resetState(): void {
    this.taskLists.set([]);
    this.selectedTaskList.set(null);
    this.taskListForItems.set(null);
    this.taskListToDelete.set(null);
    this.formVisible.set(false);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
