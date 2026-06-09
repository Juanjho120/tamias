import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-cancel-reservation-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './cancel-reservation-modal.component.html'
})
export class CancelReservationModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() reservationTitle = '';
  @Input() loading = false;

  @Output() confirm = new EventEmitter<{ reason: string | null }>();
  @Output() cancel = new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    reason: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.form.reset({ reason: '' });
    }
  }

  submit(): void {
    const rawValue = this.form.getRawValue();

    this.confirm.emit({
      reason: rawValue.reason.trim() || null
    });
  }
}
