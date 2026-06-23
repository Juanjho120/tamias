import { DOCUMENT } from '@angular/common';
import { Injectable, NgZone, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class IconActionButtonAutoEnhancerService {
  private readonly document = inject(DOCUMENT);
  private readonly ngZone = inject(NgZone);

  private observer?: MutationObserver;
  private scheduled = false;
  private started = false;

  private readonly styles = `
    .btn-icon-action-auto {
      display: inline-flex !important;
      align-items: center !important;
      justify-content: center !important;
      gap: 0 !important;
      width: 2rem;
      min-width: 2rem;
      height: 2rem;
      padding: 0 !important;
      font-size: 0 !important;
      line-height: 1 !important;
      white-space: nowrap;
    }

    .btn-icon-action-auto.btn-sm {
      width: 1.875rem;
      min-width: 1.875rem;
      height: 1.875rem;
    }

    .btn-icon-action-auto.btn-lg {
      width: 2.5rem;
      min-width: 2.5rem;
      height: 2.5rem;
    }

    .btn-icon-action-auto > i.bi,
    .btn-icon-action-auto > .bi,
    .btn-icon-action-auto > svg,
    .btn-icon-action-auto > .spinner-border {
      margin: 0 !important;
      font-size: 1rem !important;
      line-height: 1 !important;
    }

    .btn-icon-action-auto.btn-lg > i.bi,
    .btn-icon-action-auto.btn-lg > .bi,
    .btn-icon-action-auto.btn-lg > svg {
      font-size: 1.15rem !important;
    }

    .btn-icon-action-auto > .spinner-border {
      width: 1rem;
      height: 1rem;
    }
  `;

  private readonly iconByNormalizedLabel = new Map<string, string>([
    ['imagenes', 'bi-images'],
    ['images', 'bi-images'],
    ['editar', 'bi-pencil-square'],
    ['edit', 'bi-pencil-square'],
    ['eliminar', 'bi-trash'],
    ['delete', 'bi-trash'],
    ['borrar', 'bi-trash'],
    ['modelos 3d de cajas', 'bi-box-seam'],
    ['modelo 3d de caja', 'bi-box-seam'],
    ['3d box models', 'bi-box-seam'],
    ['3d box model', 'bi-box-seam'],
    ['product box', 'bi-box-seam'],
    ['caras', 'bi-grid-3x3-gap'],
    ['faces', 'bi-grid-3x3-gap'],
    ['subir original', 'bi-upload'],
    ['upload original', 'bi-upload'],
    ['subir imagen', 'bi-cloud-arrow-up'],
    ['upload image', 'bi-cloud-arrow-up'],
    ['upload logo', 'bi-cloud-arrow-up'],
    ['subir logo', 'bi-cloud-arrow-up'],
    ['reemplazar', 'bi-arrow-repeat'],
    ['replace', 'bi-arrow-repeat'],
    ['reemplazar logo', 'bi-arrow-repeat'],
    ['replace logo', 'bi-arrow-repeat'],
    ['detectar contorno', 'bi-bounding-box-circles'],
    ['detect contour', 'bi-bounding-box-circles'],
    ['items', 'bi-list-ul'],
    ['tareas', 'bi-check2-square'],
    ['tasks', 'bi-check2-square'],
    ['detalles', 'bi-info-circle'],
    ['details', 'bi-info-circle'],
    ['supplies', 'bi-box2-heart'],
    ['cancelar', 'bi-x-lg'],
    ['cancel', 'bi-x-lg'],
    ['cerrar', 'bi-x-lg'],
    ['close', 'bi-x-lg'],
    ['desactivar', 'bi-pause-circle'],
    ['deactivate', 'bi-pause-circle'],
    ['activar', 'bi-check-circle'],
    ['activate', 'bi-check-circle'],
    ['abrir', 'bi-box-arrow-up-right'],
    ['open', 'bi-box-arrow-up-right'],
    ['procesar', 'bi-cpu'],
    ['process', 'bi-cpu'],
    ['indexar', 'bi-database-add'],
    ['index', 'bi-database-add'],
    ['chunks', 'bi-braces'],
    ['historial', 'bi-clock-history'],
    ['history', 'bi-clock-history'],
    ['reprogramar', 'bi-calendar2-plus'],
    ['reschedule', 'bi-calendar2-plus'],
    ['pausar', 'bi-pause-fill'],
    ['pause', 'bi-pause-fill'],
    ['generar registro', 'bi-journal-plus'],
    ['generate record', 'bi-journal-plus'],
    ['membresias', 'bi-diagram-3'],
    ['memberships', 'bi-diagram-3'],
    ['guardar', 'bi-check-lg'],
    ['save', 'bi-check-lg'],
    ['confirmar', 'bi-check-lg'],
    ['confirm', 'bi-check-lg'],
    ['nuevo', 'bi-plus-lg'],
    ['new', 'bi-plus-lg'],
    ['nueva organizacion', 'bi-plus-lg'],
    ['new organization', 'bi-plus-lg'],
    ['nuevo usuario', 'bi-plus-lg'],
    ['new user', 'bi-plus-lg']
  ]);

  start(): void {
    if (this.started || typeof MutationObserver === 'undefined') {
      return;
    }

    this.started = true;
    this.injectStyles();

    this.ngZone.runOutsideAngular(() => {
      window.setTimeout(() => this.enhanceAll(), 0);

      this.observer = new MutationObserver(() => this.scheduleEnhance());
      this.observer.observe(this.document.body, {
        childList: true,
        subtree: true
      });
    });
  }

  private scheduleEnhance(): void {
    if (this.scheduled) {
      return;
    }

    this.scheduled = true;
    window.setTimeout(() => {
      this.scheduled = false;
      this.enhanceAll();
    }, 80);
  }

  private enhanceAll(): void {
    const buttons = Array.from(this.document.querySelectorAll<HTMLButtonElement | HTMLAnchorElement>('button.btn, a.btn'));
    buttons.forEach((button) => this.enhanceButton(button));
  }

  private enhanceButton(button: HTMLButtonElement | HTMLAnchorElement): void {
    if (button.classList.contains('btn-close') || button.dataset['tamiasIconAction'] === 'false') {
      return;
    }

    if (!this.isActionScope(button)) {
      return;
    }

    const label = this.resolveLabel(button);
    if (!label) {
      return;
    }

    this.ensureIcon(button, label);

    if (!this.hasActionIcon(button)) {
      return;
    }

    if (!button.getAttribute('title')) {
      button.setAttribute('title', label);
    }

    if (!button.getAttribute('aria-label')) {
      button.setAttribute('aria-label', label);
    }

    button.classList.add('btn-icon-action-auto');
    button.dataset['tamiasIconAction'] = 'true';
  }

  private isActionScope(button: HTMLElement): boolean {
    return Boolean(button.closest('.table, .table-responsive, .modal, .btn-group, .dropdown-menu, [data-tamias-action-scope]'));
  }

  private resolveLabel(button: HTMLElement): string {
    const explicitLabel = button.getAttribute('aria-label')
      ?? button.getAttribute('title')
      ?? button.getAttribute('data-label')
      ?? button.getAttribute('data-tamias-label');

    if (explicitLabel?.trim()) {
      return this.normalizeWhitespace(explicitLabel);
    }

    const clone = button.cloneNode(true) as HTMLElement;
    clone.querySelectorAll('i, svg, .spinner-border, .visually-hidden').forEach((node) => node.remove());
    return this.normalizeWhitespace(clone.textContent ?? '');
  }

  private ensureIcon(button: HTMLElement, label: string): void {
    if (this.hasActionIcon(button)) {
      return;
    }

    const iconClass = this.resolveIconClass(label);
    if (!iconClass) {
      return;
    }

    const icon = this.document.createElement('i');
    icon.className = `bi ${iconClass}`;
    icon.setAttribute('aria-hidden', 'true');
    button.prepend(icon);
  }

  private hasActionIcon(button: HTMLElement): boolean {
    return Boolean(button.querySelector('i.bi, .bi, svg, .spinner-border'));
  }

  private resolveIconClass(label: string): string | null {
    const normalized = this.normalizeLabel(label);
    return this.iconByNormalizedLabel.get(normalized) ?? null;
  }

  private normalizeLabel(value: string): string {
    return this.normalizeWhitespace(value)
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[¿?¡!:.]/g, '')
      .toLowerCase();
  }

  private normalizeWhitespace(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }

  private injectStyles(): void {
    if (this.document.getElementById('tamias-icon-action-auto-styles')) {
      return;
    }

    const style = this.document.createElement('style');
    style.id = 'tamias-icon-action-auto-styles';
    style.textContent = this.styles;
    this.document.head.appendChild(style);
  }
}
