import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ScheduledMaintenanceHistory } from '../../models/scheduled-maintenance.model';

@Component({
  selector: 'app-scheduled-maintenance-history-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './scheduled-maintenance-history-modal.component.html'
})
export class ScheduledMaintenanceHistoryModalComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() loading = false;
  @Input() history: ScheduledMaintenanceHistory[] = [];

  @Output() close = new EventEmitter<void>();
}
