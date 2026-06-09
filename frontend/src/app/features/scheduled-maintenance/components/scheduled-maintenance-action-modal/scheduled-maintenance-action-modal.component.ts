import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

export type ScheduledMaintenanceActionType = 'pause' | 'resume' | 'cancel' | 'reschedule';

@Component({
  selector: 'app-scheduled-maintenance-action-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './scheduled-maintenance-action-modal.component.html'
})
export class ScheduledMaintenanceActionModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() action: ScheduledMaintenanceActionType | null = null;
  @Input() title = '';
  @Input() loading = false;

  @Output() confirm = new EventEmitter<{ nextDueDate: string | null; reason: string | null }>();
  @Output() cancel = new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    nextDueDate: [''],
    reason: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] || changes['action']) {
      this.form.reset({
        nextDueDate: '',
        reason: ''
      });

      const nextDueDateControl = this.form.controls.nextDueDate;

      if (this.action === 'reschedule') {
        nextDueDateControl.addValidators([Validators.required]);
      } else {
        nextDueDateControl.clearValidators();
      }

      nextDueDateControl.updateValueAndValidity();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.confirm.emit({
      nextDueDate: rawValue.nextDueDate || null,
      reason: rawValue.reason.trim() || null
    });
  }
}
