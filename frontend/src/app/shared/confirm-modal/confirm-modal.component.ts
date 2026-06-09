import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  templateUrl: './confirm-modal.component.html'
})
export class ConfirmModalComponent {
  @Input() open = false;
  @Input() title = 'Confirm action';
  @Input() message = 'Are you sure you want to continue?';
  @Input() confirmText = 'Confirm';
  @Input() cancelText = 'Cancel';
  @Input() loading = false;
  @Input() confirmButtonClass = 'btn-danger';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
