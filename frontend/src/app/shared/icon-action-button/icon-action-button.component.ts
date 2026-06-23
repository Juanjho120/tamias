import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-icon-action-button',
  standalone: true,
  imports: [NgClass, TranslatePipe],
  template: `
    <button
      type="button"
      class="btn btn-icon-action"
      [ngClass]="buttonClasses"
      [disabled]="disabled || loading"
      [attr.title]="labelKey | translate"
      [attr.aria-label]="labelKey | translate"
      (click)="action.emit()">
      @if (loading) {
        <span class="spinner-border spinner-border-sm" aria-hidden="true"></span>
      } @else {
        <i class="bi" [ngClass]="icon" aria-hidden="true"></i>
      }
      <span class="visually-hidden">{{ labelKey | translate }}</span>
    </button>
  `,
  styles: [
    `
      .btn-icon-action {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 0;
        width: 2rem;
        min-width: 2rem;
        height: 2rem;
        padding: 0 !important;
        line-height: 1;
      }

      .btn-icon-action.btn-sm {
        width: 1.875rem;
        min-width: 1.875rem;
        height: 1.875rem;
      }

      .btn-icon-action.btn-lg {
        width: 2.5rem;
        min-width: 2.5rem;
        height: 2.5rem;
      }

      .btn-icon-action .bi {
        font-size: 1rem;
        line-height: 1;
      }

      .btn-icon-action.btn-lg .bi {
        font-size: 1.15rem;
      }
    `
  ]
})
export class IconActionButtonComponent {
  @Input({ required: true }) icon = '';
  @Input({ required: true }) labelKey = '';
  @Input() variant: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'light' | 'dark' | 'outline-primary' | 'outline-secondary' | 'outline-success' | 'outline-danger' | 'outline-warning' | 'outline-info' | 'outline-light' | 'outline-dark' = 'outline-secondary';
  @Input() size: 'sm' | 'md' | 'lg' = 'sm';
  @Input() disabled = false;
  @Input() loading = false;

  @Output() action = new EventEmitter<void>();

  get buttonClasses(): string[] {
    const classes = [`btn-${this.variant}`];

    if (this.size !== 'md') {
      classes.push(`btn-${this.size}`);
    }

    return classes;
  }
}
