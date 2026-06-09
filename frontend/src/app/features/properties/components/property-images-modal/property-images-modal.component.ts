import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PropertySummary } from '../../models/property.model';
import { PropertyImage } from '../../models/property-image.model';
import { PropertyImageService } from '../../services/property-image.service';

@Component({
  selector: 'app-property-images-modal',
  standalone: true,
  imports: [DatePipe, DecimalPipe, FormsModule, TranslatePipe, ConfirmModalComponent],
  templateUrl: './property-images-modal.component.html'
})
export class PropertyImagesModalComponent implements OnChanges {
  private readonly propertyImageService = inject(PropertyImageService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  @Input() open = false;
  @Input() property: PropertySummary | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() imagesChanged = new EventEmitter<void>();

  readonly images = signal<PropertyImage[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly settingCoverId = signal<string | null>(null);
  readonly deletingId = signal<string | null>(null);
  readonly imageToDelete = signal<PropertyImage | null>(null);

  readonly selectedFile = signal<File | null>(null);
  readonly uploadAsCover = signal(false);
  readonly selectedPreviewUrl = signal<string | null>(null);

  readonly deleteMessage = computed(() => {
    const image = this.imageToDelete();

    if (!image) {
      return '';
    }

    return this.languageService.instant('properties.images.confirmDeleteMessage', {
      filename: image.originalFilename
    });
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['property']) && this.open && this.property) {
      this.loadImages();
    }
  }

  requestClose(): void {
    if (this.uploading() || this.deletingId() || this.settingCoverId()) {
      return;
    }

    this.clearUploadSelection();
    this.images.set([]);
    this.close.emit();
  }

  loadImages(): void {
    const property = this.property;

    if (!property) {
      return;
    }

    this.loading.set(true);

    this.propertyImageService.findAll(property.id).subscribe({
      next: (images: PropertyImage[]) => {
        this.images.set(this.sortImages(images));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.images.messages.loadError')));
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.revokeSelectedPreview();
    this.selectedFile.set(file);

    if (file) {
      this.selectedPreviewUrl.set(URL.createObjectURL(file));
    }
  }

  upload(): void {
    const property = this.property;
    const file = this.selectedFile();

    if (!property || !file) {
      this.toastService.warning(this.languageService.instant('properties.images.messages.fileRequired'));
      return;
    }

    this.uploading.set(true);

    this.propertyImageService.upload(property.id, file, this.uploadAsCover()).subscribe({
      next: () => {
        this.uploading.set(false);
        this.toastService.success(this.languageService.instant('properties.images.messages.uploaded'));
        this.clearUploadSelection();
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.images.messages.uploadError')));
      }
    });
  }

  setCover(image: PropertyImage): void {
    const property = this.property;

    if (!property || image.cover) {
      return;
    }

    this.settingCoverId.set(image.id);

    this.propertyImageService.setCover(property.id, image.id).subscribe({
      next: () => {
        this.settingCoverId.set(null);
        this.toastService.success(this.languageService.instant('properties.images.messages.coverUpdated'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.settingCoverId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.images.messages.coverError')));
      }
    });
  }

  requestDelete(image: PropertyImage): void {
    this.imageToDelete.set(image);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.imageToDelete.set(null);
  }

  confirmDelete(): void {
    const property = this.property;
    const image = this.imageToDelete();

    if (!property || !image) {
      return;
    }

    this.deletingId.set(image.id);

    this.propertyImageService.delete(property.id, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.imageToDelete.set(null);
        this.toastService.success(this.languageService.instant('properties.images.messages.deleted'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('properties.images.messages.deleteError')));
      }
    });
  }

  clearUploadSelection(): void {
    this.selectedFile.set(null);
    this.uploadAsCover.set(false);
    this.revokeSelectedPreview();

    const input = document.getElementById('property-image-file') as HTMLInputElement | null;

    if (input) {
      input.value = '';
    }
  }

  trackByImageId(index: number, image: PropertyImage): string {
    return image.id;
  }

  private sortImages(images: PropertyImage[]): PropertyImage[] {
    return [...images].sort((a, b) => {
      if (a.cover && !b.cover) {
        return -1;
      }

      if (!a.cover && b.cover) {
        return 1;
      }

      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }

  private revokeSelectedPreview(): void {
    const previewUrl = this.selectedPreviewUrl();

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      this.selectedPreviewUrl.set(null);
    }
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
