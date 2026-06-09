import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-ai-session-title-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './ai-session-title-modal.component.html'
})
export class AiSessionTitleModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() title = '';
  @Input() loading = false;

  @Output() save = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<void>();

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(150)]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.form.reset({
        title: this.title
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.save.emit(this.form.getRawValue().title.trim());
  }
}
