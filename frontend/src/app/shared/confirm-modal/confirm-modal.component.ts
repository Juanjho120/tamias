import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './confirm-modal.component.html'
})
export class ConfirmModalComponent {
  @Input() open = false;
  @Input() title = 'common.confirmAction';
  @Input() message = 'common.confirmContinue';
  @Input() confirmText = 'common.confirm';
  @Input() cancelText = 'common.cancel';
  @Input() loading = false;
  @Input() confirmButtonClass = 'btn-danger';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}
