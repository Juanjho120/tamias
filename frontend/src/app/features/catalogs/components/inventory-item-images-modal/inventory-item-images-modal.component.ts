import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { CatalogItem } from '../../models/catalog.model';
import { InventoryItemImage } from '../../models/inventory-item-image.model';
import { InventoryItemImageService } from '../../services/inventory-item-image.service';

@Component({
  selector: 'app-inventory-item-images-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ConfirmModalComponent],
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
  private readonly languageService = inject(LanguageService);

  readonly images = signal<InventoryItemImage[]>([]);
  readonly selectedFiles = signal<File[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly settingCoverId = signal<string | null>(null);
  readonly imageToDelete = signal<InventoryItemImage | null>(null);
  readonly deleteMessage = computed(() => {
    const image = this.imageToDelete();

    if (!image) {
      return '';
    }

    return this.languageService.instant('catalogs.items.inventoryItems.images.confirmDeleteMessage', {
      filename: image.originalFilename
    });
  });

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

  filesSummary(): string {
    const files = this.selectedFiles();

    if (files.length === 0) {
      return this.languageService.instant('catalogs.items.inventoryItems.images.upload.noFilesSelected');
    }

    if (files.length === 1) {
      return files[0].name;
    }

    return this.languageService.instant('catalogs.items.inventoryItems.images.upload.selectedFiles', { count: files.length });
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
    forkJoin(
      files.map((file, index) =>
        this.imageService.upload(this.inventoryItem!.id, file, shouldMarkFirstAsCover && index === 0)
      )
    ).subscribe({
      next: () => {
        this.uploading.set(false);
        this.selectedFiles.set([]);
        this.toastService.success(this.languageService.instant('catalogs.items.inventoryItems.images.messages.uploaded'));
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('catalogs.items.inventoryItems.images.messages.uploadError'))
        );
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
        this.toastService.success(this.languageService.instant('catalogs.items.inventoryItems.images.messages.coverUpdated'));
        this.loadImages();
      },
      error: (error: unknown) => {
        this.settingCoverId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('catalogs.items.inventoryItems.images.messages.coverError'))
        );
      }
    });
  }

  requestDelete(image: InventoryItemImage): void {
    if (this.deletingId()) {
      return;
    }

    this.imageToDelete.set(image);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.imageToDelete.set(null);
  }

  confirmDelete(): void {
    const image = this.imageToDelete();

    if (!this.inventoryItem?.id || !image || this.deletingId()) {
      return;
    }

    this.deletingId.set(image.id);
    this.imageService.delete(this.inventoryItem.id, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.imageToDelete.set(null);
        this.images.update((current) => current.filter((item) => item.id !== image.id));
        this.toastService.success(this.languageService.instant('catalogs.items.inventoryItems.images.messages.deleted'));
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('catalogs.items.inventoryItems.images.messages.deleteError'))
        );
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
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('catalogs.items.inventoryItems.images.messages.loadError'))
        );
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
    this.imageToDelete.set(null);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };

    return maybeHttpError.error?.message ?? fallback;
  }
}
