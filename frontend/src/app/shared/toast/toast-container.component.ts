import { NgClass } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ToastMessage } from './toast.model';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="tamias-toast-container position-fixed top-0 end-0 p-3">
      @for (toast of toastService.messages(); track toast.id) {
        <div
          class="toast show border-0 shadow-sm mb-2"
          role="alert"
          aria-live="assertive"
          aria-atomic="true">
          <div class="toast-header" [ngClass]="headerClass(toast)">
            <i class="bi me-2" [ngClass]="iconClass(toast)"></i>
            <strong class="me-auto">{{ toast.title || defaultTitle(toast) }}</strong>
            <button
              type="button"
              class="btn-close"
              aria-label="Close"
              (click)="toastService.dismiss(toast.id)">
            </button>
          </div>
          <div class="toast-body bg-white">
            {{ toast.message }}
          </div>
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  readonly toastService = inject(ToastService);

  headerClass(toast: ToastMessage): string {
    switch (toast.type) {
      case 'success':
        return 'bg-success-subtle text-success-emphasis';
      case 'danger':
        return 'bg-danger-subtle text-danger-emphasis';
      case 'warning':
        return 'bg-warning-subtle text-warning-emphasis';
      case 'info':
        return 'bg-info-subtle text-info-emphasis';
    }
  }

  iconClass(toast: ToastMessage): string {
    switch (toast.type) {
      case 'success':
        return 'bi-check-circle-fill';
      case 'danger':
        return 'bi-exclamation-triangle-fill';
      case 'warning':
        return 'bi-exclamation-circle-fill';
      case 'info':
        return 'bi-info-circle-fill';
    }
  }

  defaultTitle(toast: ToastMessage): string {
    switch (toast.type) {
      case 'success':
        return 'Success';
      case 'danger':
        return 'Error';
      case 'warning':
        return 'Warning';
      case 'info':
        return 'Info';
    }
  }
}
