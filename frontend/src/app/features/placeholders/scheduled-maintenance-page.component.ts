import { Component } from '@angular/core';

@Component({
  selector: 'app-scheduled-maintenance-page',
  standalone: true,
  template: `
    <div class="card page-card">
      <div class="card-body p-4">
        <div class="d-flex align-items-center gap-3 mb-3">
          <div class="d-inline-flex align-items-center justify-content-center rounded-3 bg-secondary-subtle text-secondary p-3">
            <i class="bi bi-layout-text-window-reverse fs-4"></i>
          </div>
          <div>
            <h1 class="h4 mb-1">Scheduled Maintenance</h1>
            <p class="text-muted mb-0">Scheduled maintenance, history and calendar will be implemented here.</p>
          </div>
        </div>
        <div class="alert alert-info mb-0">
          This route is already protected and ready for the next implementation block.
        </div>
      </div>
    </div>
  `
})
export class ScheduledMaintenancePageComponent {
}
