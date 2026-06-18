import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';

import { ApiError } from '../../../../core/models/api-error.model';
import { ToastService } from '../../../../shared/toast/toast.service';
import { CatalogItem } from '../../models/catalog.model';
import { InventoryItemImage } from '../../models/inventory-item-image.model';
import { InventoryItemImageService } from '../../services/inventory-item-image.service';

@Component({
  selector: 'app-inventory-item-images-modal',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './inventory-item-images-modal.component.html',
  styles: [
    `
      .inventory-item-image-card {
        min-height: 100%;
      }

      .inventory-item-image-preview {
        height: 180px;
        object-fit: cover;
        width: 100%;
      }
    `
  ]
})
export class InventoryItemImagesModalComponent implements OnChanges {
  @Input() open = false;
  @Input() inventoryItem: CatalogItem | null = null;
  @Output() closed = new EventEmitter<void>();

  private readonly imageService = inject(InventoryItemImageService);
  private readonly toastService = inject(ToastService);

  readonly images = signal<InventoryItemImage[]>([]);
  readonly selectedFiles = signal<File[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly settingCoverId = signal<string | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['inventoryItem']) && this.open && this.inventoryItem?.id) {
      this.loadImages();
      return;
    }

    if (changes['open'] && !this.open) {
      this.resetState();
    }
  }

  itemDisplayName(): string {
    if (!this.inventoryItem) {
      return '—';
    }

    const itemName = this.inventoryItem.name ?? '—';
    const brandName = this.inventoryItem.brandName;

    return brandName ? `${itemName} - ${brandName}` : itemName;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFiles.set(Array.from(input.files ?? []));
  }

  uploadSelected(): void {
    if (!this.inventoryItem?.id || this.selectedFiles().length === 0 || this.uploading()) {
      return;
    }

    const files = this.selectedFiles();
    const shouldMarkFirstAsCover = this.images().length === 0;

    this.uploading.set(true);

    forkJoin(files.map((file, index) =>
      this.imageService.upload(this.inventoryItem!.id, file, shouldMarkFirstAsCover && index === 0)
    )).subscribe({
      next: () => {
        this.uploading.set(false);
        this.selectedFiles.set([]);
        this.toastService.success('Imágenes cargadas correctamente.');
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudieron cargar las imágenes.'));
      }
    });
  }

  setCover(image: InventoryItemImage): void {
    if (!this.inventoryItem?.id || image.cover || this.settingCoverId()) {
      return;
    }

    this.settingCoverId.set(image.id);

    this.imageService.setCover(this.inventoryItem.id, image.id).subscribe({
      next: () => {
        this.settingCoverId.set(null);
        this.toastService.success('Imagen principal actualizada.');
        this.loadImages();
      },
      error: (error: unknown) => {
        this.settingCoverId.set(null);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo actualizar la imagen principal.'));
      }
    });
  }

  deleteImage(image: InventoryItemImage): void {
    if (!this.inventoryItem?.id || this.deletingId()) {
      return;
    }

    const confirmed = window.confirm(`¿Eliminar la imagen ${image.originalFilename}?`);

    if (!confirmed) {
      return;
    }

    this.deletingId.set(image.id);

    this.imageService.delete(this.inventoryItem.id, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.images.update((current) => current.filter((item) => item.id !== image.id));
        this.toastService.success('Imagen eliminada correctamente.');
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudo eliminar la imagen.'));
      }
    });
  }

  close(): void {
    if (this.uploading() || this.deletingId() || this.settingCoverId()) {
      return;
    }

    this.closed.emit();
  }

  private loadImages(): void {
    if (!this.inventoryItem?.id) {
      this.images.set([]);
      return;
    }

    this.loading.set(true);

    this.imageService.findAll(this.inventoryItem.id).subscribe({
      next: (images) => {
        this.images.set(images);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, 'No se pudieron cargar las imágenes del item.'));
      }
    });
  }

  private resetState(): void {
    this.images.set([]);
    this.selectedFiles.set([]);
    this.loading.set(false);
    this.uploading.set(false);
    this.deletingId.set(null);
    this.settingCoverId.set(null);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
