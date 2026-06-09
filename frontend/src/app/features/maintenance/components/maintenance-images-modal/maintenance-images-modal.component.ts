import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceImage } from '../../models/maintenance-image.model';
import { MaintenanceRecordSummary } from '../../models/maintenance-record.model';
import { MaintenanceImageService } from '../../services/maintenance-image.service';

@Component({
  selector: 'app-maintenance-images-modal',
  standalone: true,
  imports: [DatePipe, DecimalPipe, TranslatePipe, ConfirmModalComponent],
  templateUrl: './maintenance-images-modal.component.html'
})
export class MaintenanceImagesModalComponent implements OnChanges {
  private readonly maintenanceImageService = inject(MaintenanceImageService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  @Input() open = false;
  @Input() maintenanceRecord: MaintenanceRecordSummary | null = null;

  @Output() close = new EventEmitter<void>();
  @Output() imagesChanged = new EventEmitter<void>();

  readonly images = signal<MaintenanceImage[]>([]);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly imageToDelete = signal<MaintenanceImage | null>(null);

  readonly selectedFile = signal<File | null>(null);
  readonly selectedPreviewUrl = signal<string | null>(null);

  readonly deleteMessage = computed(() => {
    const image = this.imageToDelete();

    if (!image) {
      return '';
    }

    return this.languageService.instant('maintenance.images.confirmDeleteMessage', {
      filename: image.originalFilename
    });
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['maintenanceRecord']) && this.open && this.maintenanceRecord) {
      this.loadImages();
    }
  }

  requestClose(): void {
    if (this.uploading() || this.deletingId()) {
      return;
    }

    this.clearUploadSelection();
    this.images.set([]);
    this.close.emit();
  }

  loadImages(): void {
    const maintenanceRecord = this.maintenanceRecord;

    if (!maintenanceRecord) {
      return;
    }

    this.loading.set(true);

    this.maintenanceImageService.findAll(maintenanceRecord.id).subscribe({
      next: (images: MaintenanceImage[]) => {
        this.images.set(this.sortImages(images));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.images.messages.loadError')));
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
    const maintenanceRecord = this.maintenanceRecord;
    const file = this.selectedFile();

    if (!maintenanceRecord || !file) {
      this.toastService.warning(this.languageService.instant('maintenance.images.messages.fileRequired'));
      return;
    }

    this.uploading.set(true);

    this.maintenanceImageService.upload(maintenanceRecord.id, file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.toastService.success(this.languageService.instant('maintenance.images.messages.uploaded'));
        this.clearUploadSelection();
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.images.messages.uploadError')));
      }
    });
  }

  requestDelete(image: MaintenanceImage): void {
    this.imageToDelete.set(image);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.imageToDelete.set(null);
  }

  confirmDelete(): void {
    const maintenanceRecord = this.maintenanceRecord;
    const image = this.imageToDelete();

    if (!maintenanceRecord || !image) {
      return;
    }

    this.deletingId.set(image.id);

    this.maintenanceImageService.delete(maintenanceRecord.id, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.imageToDelete.set(null);
        this.toastService.success(this.languageService.instant('maintenance.images.messages.deleted'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.images.messages.deleteError')));
      }
    });
  }

  clearUploadSelection(): void {
    this.selectedFile.set(null);
    this.revokeSelectedPreview();

    const input = document.getElementById('maintenance-image-file') as HTMLInputElement | null;

    if (input) {
      input.value = '';
    }
  }

  trackByImageId(index: number, image: MaintenanceImage): string {
    return image.id;
  }

  private sortImages(images: MaintenanceImage[]): MaintenanceImage[] {
    return [...images].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
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
