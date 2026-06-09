import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { TaskItem, TaskList, TaskListSummary } from '../../models/task-list.model';
import { TaskTemplateOption } from '../../models/task-reference.model';
import { TaskListService } from '../../services/task-list.service';

@Component({
  selector: 'app-task-items-modal',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, TranslatePipe, ConfirmModalComponent],
  templateUrl: './task-items-modal.component.html'
})
export class TaskItemsModalComponent implements OnChanges {
  private readonly taskListService = inject(TaskListService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() taskListSummary: TaskListSummary | null = null;
  @Input() taskTemplates: TaskTemplateOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() itemsChanged = new EventEmitter<void>();

  readonly taskList = signal<TaskList | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly itemToDelete = signal<TaskItem | null>(null);
  readonly editingItem = signal<TaskItem | null>(null);

  readonly itemForm = this.formBuilder.nonNullable.group({
    taskTemplateId: [''],
    taskName: ['', [Validators.required, Validators.maxLength(150)]],
    responsiblePerson: ['', [Validators.maxLength(150)]],
    completed: [false],
    sortOrder: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['taskListSummary']) && this.open && this.taskListSummary) {
      this.loadTaskList();
    }
  }

  requestClose(): void {
    if (this.loading() || this.saving() || this.deletingId()) {
      return;
    }

    this.resetState();
    this.close.emit();
  }

  loadTaskList(): void {
    const summary = this.taskListSummary;

    if (!summary) {
      return;
    }

    this.loading.set(true);

    this.taskListService.findById(summary.id).subscribe({
      next: (taskList) => {
        this.taskList.set({
          ...taskList,
          items: [...taskList.items].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        });
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.items.messages.loadError')));
      }
    });
  }

  saveItem(): void {
    const taskList = this.taskList();

    if (!taskList) {
      return;
    }

    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }

    const rawValue = this.itemForm.getRawValue();
    const request = {
      taskTemplateId: rawValue.taskTemplateId || null,
      taskName: rawValue.taskName.trim(),
      responsiblePerson: rawValue.responsiblePerson.trim() || null,
      completed: rawValue.completed,
      sortOrder: rawValue.sortOrder === '' ? null : Number(rawValue.sortOrder)
    };

    this.saving.set(true);

    const editingItem = this.editingItem();

    const saveRequest = editingItem
      ? this.taskListService.updateItem(taskList.id, editingItem.id, request)
      : this.taskListService.createItem(taskList.id, request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          editingItem
            ? this.languageService.instant('tasks.items.messages.updated')
            : this.languageService.instant('tasks.items.messages.created')
        );
        this.cancelEdit();
        this.itemsChanged.emit();
        this.loadTaskList();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.items.messages.saveError')));
      }
    });
  }

  editItem(item: TaskItem): void {
    this.editingItem.set(item);
    this.itemForm.reset({
      taskTemplateId: item.taskTemplateId ?? '',
      taskName: item.taskName,
      responsiblePerson: item.responsiblePerson ?? '',
      completed: !!item.completed,
      sortOrder: item.sortOrder !== null && item.sortOrder !== undefined ? String(item.sortOrder) : ''
    });
  }

  cancelEdit(): void {
    this.editingItem.set(null);
    this.itemForm.reset({
      taskTemplateId: '',
      taskName: '',
      responsiblePerson: '',
      completed: false,
      sortOrder: ''
    });
  }

  toggleCompletion(item: TaskItem): void {
    const taskList = this.taskList();

    if (!taskList) {
      return;
    }

    this.taskListService.updateItemCompletion(taskList.id, item.id, {
      completed: !item.completed
    }).subscribe({
      next: () => {
        this.toastService.success(this.languageService.instant('tasks.items.messages.completionUpdated'));
        this.itemsChanged.emit();
        this.loadTaskList();
      },
      error: (error: unknown) => {
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.items.messages.completionError')));
      }
    });
  }

  requestDeleteItem(item: TaskItem): void {
    this.itemToDelete.set(item);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.itemToDelete.set(null);
  }

  confirmDelete(): void {
    const taskList = this.taskList();
    const item = this.itemToDelete();

    if (!taskList || !item) {
      return;
    }

    this.deletingId.set(item.id);

    this.taskListService.deleteItem(taskList.id, item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.itemToDelete.set(null);
        this.toastService.success(this.languageService.instant('tasks.items.messages.deleted'));
        this.itemsChanged.emit();
        this.loadTaskList();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('tasks.items.messages.deleteError')));
      }
    });
  }

  onTemplateSelected(templateId: string): void {
    if (!templateId || this.itemForm.controls.taskName.value) {
      return;
    }

    const template = this.taskTemplates.find((item) => item.id === templateId);

    if (template) {
      this.itemForm.controls.taskName.setValue(template.name);
    }
  }

  isItemInvalid(controlName: keyof typeof this.itemForm.controls): boolean {
    const control = this.itemForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.taskList.set(null);
    this.itemToDelete.set(null);
    this.cancelEdit();
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
