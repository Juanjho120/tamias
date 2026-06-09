import { DatePipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  TaskItemRequest,
  TaskList,
  TaskListRequest,
  TaskListStatus,
  TASK_LIST_STATUSES
} from '../../models/task-list.model';
import {
  TaskMaintenanceRecordOption,
  TaskPropertyOption,
  TaskReservationOption,
  TaskTemplateOption
} from '../../models/task-reference.model';

@Component({
  selector: 'app-task-list-form-modal',
  standalone: true,
  imports: [DatePipe, NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './task-list-form-modal.component.html'
})
export class TaskListFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() taskList: TaskList | null = null;
  @Input() properties: TaskPropertyOption[] = [];
  @Input() reservations: TaskReservationOption[] = [];
  @Input() maintenanceRecords: TaskMaintenanceRecordOption[] = [];
  @Input() taskTemplates: TaskTemplateOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<TaskListRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = TASK_LIST_STATUSES.filter((status) => status !== 'DELETED');
  readonly items = signal<TaskItemRequest[]>([]);
  readonly editingItemIndex = signal<number | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: ['', [Validators.required]],
    reservationId: [''],
    maintenanceRecordId: [''],
    title: ['', [Validators.required, Validators.maxLength(150)]],
    creationDate: [''],
    dueDate: [''],
    status: this.formBuilder.nonNullable.control<TaskListStatus>('OPEN', [Validators.required])
  });

  readonly itemForm = this.formBuilder.nonNullable.group({
    taskTemplateId: [''],
    taskName: ['', [Validators.required, Validators.maxLength(150)]],
    responsiblePerson: ['', [Validators.maxLength(150)]],
    completed: [false],
    sortOrder: ['']
  });

  readonly selectedPropertyReservations = computed(() => {
    const propertyId = this.form.controls.propertyId.value;

    if (!propertyId) {
      return this.reservations;
    }

    return this.reservations.filter((reservation) => reservation.propertyId === propertyId);
  });

  readonly selectedPropertyMaintenanceRecords = computed(() => {
    const propertyId = this.form.controls.propertyId.value;

    if (!propertyId) {
      return this.maintenanceRecords;
    }

    return this.maintenanceRecords.filter((record) => record.propertyId === propertyId);
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['taskList'] || changes['open']) {
      this.patchForm();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.save.emit({
      propertyId: rawValue.propertyId,
      reservationId: rawValue.reservationId || null,
      maintenanceRecordId: rawValue.maintenanceRecordId || null,
      title: rawValue.title.trim(),
      creationDate: rawValue.creationDate || null,
      dueDate: rawValue.dueDate || null,
      status: rawValue.status,
      items: this.items()
    });
  }

  addOrUpdateItem(): void {
    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }

    const rawValue = this.itemForm.getRawValue();
    const item: TaskItemRequest = {
      taskTemplateId: rawValue.taskTemplateId || null,
      taskName: rawValue.taskName.trim(),
      responsiblePerson: rawValue.responsiblePerson.trim() || null,
      completed: rawValue.completed,
      sortOrder: rawValue.sortOrder === '' ? null : Number(rawValue.sortOrder)
    };

    const currentItems = [...this.items()];
    const index = this.editingItemIndex();

    if (index === null) {
      item.sortOrder = item.sortOrder ?? currentItems.length + 1;
      currentItems.push(item);
    } else {
      currentItems[index] = item;
    }

    this.items.set(currentItems);
    this.cancelItemEdit();
  }

  editItem(index: number): void {
    const item = this.items()[index];

    if (!item) {
      return;
    }

    this.editingItemIndex.set(index);
    this.itemForm.reset({
      taskTemplateId: item.taskTemplateId ?? '',
      taskName: item.taskName,
      responsiblePerson: item.responsiblePerson ?? '',
      completed: !!item.completed,
      sortOrder: item.sortOrder !== null && item.sortOrder !== undefined ? String(item.sortOrder) : ''
    });
  }

  removeItem(index: number): void {
    const currentItems = [...this.items()];
    currentItems.splice(index, 1);
    this.items.set(currentItems);
    this.cancelItemEdit();
  }

  cancelItemEdit(): void {
    this.editingItemIndex.set(null);
    this.itemForm.reset({
      taskTemplateId: '',
      taskName: '',
      responsiblePerson: '',
      completed: false,
      sortOrder: ''
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

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isItemInvalid(controlName: keyof typeof this.itemForm.controls): boolean {
    const control = this.itemForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  reservationLabel(reservation: TaskReservationOption): string {
    return `${reservation.reservationCode || reservation.id} · ${reservation.propertyName} · ${reservation.checkIn} → ${reservation.checkOut}`;
  }

  maintenanceRecordLabel(record: TaskMaintenanceRecordOption): string {
    return `${record.title} · ${record.propertyName}`;
  }

  private patchForm(): void {
    if (!this.taskList) {
      this.form.reset({
        propertyId: '',
        reservationId: '',
        maintenanceRecordId: '',
        title: '',
        creationDate: this.today(),
        dueDate: '',
        status: 'OPEN'
      });
      this.items.set([]);
      this.cancelItemEdit();
      return;
    }

    this.form.reset({
      propertyId: this.taskList.propertyId,
      reservationId: this.taskList.reservationId ?? '',
      maintenanceRecordId: this.taskList.maintenanceRecordId ?? '',
      title: this.taskList.title,
      creationDate: this.taskList.creationDate ?? '',
      dueDate: this.taskList.dueDate ?? '',
      status: this.taskList.status === 'DELETED' ? 'CANCELLED' : this.taskList.status
    });

    this.items.set((this.taskList.items ?? [])
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((item) => ({
        taskTemplateId: item.taskTemplateId,
        taskName: item.taskName,
        responsiblePerson: item.responsiblePerson,
        completed: item.completed,
        sortOrder: item.sortOrder
      })));

    this.cancelItemEdit();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
