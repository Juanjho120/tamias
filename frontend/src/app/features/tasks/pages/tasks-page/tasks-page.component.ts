import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { TaskItemsModalComponent } from '../../components/task-items-modal/task-items-modal.component';
import { TaskListFormModalComponent } from '../../components/task-list-form-modal/task-list-form-modal.component';
import {
  TaskList,
  TaskListRequest,
  TaskListStatus,
  TaskListSummary,
  TASK_LIST_STATUSES
} from '../../models/task-list.model';
import { TaskReferenceData, TaskReferenceDataService } from '../../services/task-reference-data.service';
import { TaskListService } from '../../services/task-list.service';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    TaskItemsModalComponent,
    TaskListFormModalComponent
  ],
  templateUrl: './tasks-page.component.html'
})
export class TasksPageComponent implements OnInit {
  private readonly taskListService = inject(TaskListService);
  private readonly referenceDataService = inject(TaskReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly statuses = TASK_LIST_STATUSES;

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly loadingReferences = signal(false);

  readonly taskLists = signal<TaskListSummary[]>([]);
  readonly selectedTaskList = signal<TaskList | null>(null);
  readonly taskListForItems = signal<TaskListSummary | null>(null);
  readonly taskListToDelete = signal<TaskListSummary | null>(null);

  readonly references = signal<TaskReferenceData>({
    properties: [],
    reservations: [],
    maintenanceRecords: [],
    taskTemplates: []
  });

  readonly formVisible = signal(false);
  readonly formMode = signal<FormMode>('create');

  readonly propertyId = signal('');
  readonly status = signal<TaskListStatus | ''>('');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('tasks.pagination.noItems');
    }

    return this.languageService.instant('tasks.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const taskList = this.taskListToDelete();

    if (!taskList) {
      return '';
    }

    return this.languageService.instant('tasks.confirmDeleteMessage', {
      title: taskList.title
    });
  });

  ngOnInit(): void {
    this.loadReferences();
    this.loadTaskLists();
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
    this.loading.set(true);

    this.taskListService.findAll({
      propertyId: this.propertyId() || undefined,
      status: this.status(),
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<TaskListSummary>) => {
        this.taskLists.set(response.content);
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
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadTaskLists();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.status.set('');
    this.page.set(0);
    this.loadTaskLists();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadTaskLists();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadTaskLists();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadTaskLists();
  }

  openCreateForm(): void {
    this.formMode.set('create');
    this.selectedTaskList.set(null);
    this.formVisible.set(true);
  }

  openEditForm(id: string): void {
    this.loading.set(true);

    this.taskListService.findById(id).subscribe({
      next: (taskList) => {
        this.selectedTaskList.set(taskList);
        this.formMode.set('edit');
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
    this.formMode.set('create');
  }

  saveTaskList(request: TaskListRequest): void {
    const selectedTaskList = this.selectedTaskList();

    this.saving.set(true);

    const saveRequest = this.formMode() === 'edit' && selectedTaskList
      ? this.taskListService.update(selectedTaskList.id, request)
      : this.taskListService.create(request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          this.formMode() === 'edit'
            ? this.languageService.instant('tasks.messages.updated')
            : this.languageService.instant('tasks.messages.created')
        );
        this.closeForm();
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

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
