import { DatePipe } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PurchaseImage } from '../../models/purchase-image.model';
import { PurchaseListSummary } from '../../models/purchase-list.model';
import { PurchaseImageService } from '../../services/purchase-image.service';

@Component({
  selector: 'app-purchase-images-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe],
  templateUrl: './purchase-images-modal.component.html'
})
export class PurchaseImagesModalComponent implements OnChanges {
  @Input() open = false;
  @Input() purchaseList: PurchaseListSummary | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() imagesChanged = new EventEmitter<void>();

  @ViewChild('fileInput') private readonly fileInput?: ElementRef<HTMLInputElement>;

  private readonly purchaseImageService = inject(PurchaseImageService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal<boolean>(false);
  readonly uploading = signal<boolean>(false);
  readonly deletingId = signal<string | null>(null);
  readonly images = signal<PurchaseImage[]>([]);
  readonly selectedFiles = signal<File[]>([]);

  private loadedPurchaseListId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.resetState();
      return;
    }

    const purchaseListId = this.purchaseList?.id;
    const shouldLoad =
      !!purchaseListId &&
      (changes['open'] || changes['purchaseList']) &&
      purchaseListId !== this.loadedPurchaseListId;

    if (shouldLoad) {
      this.loadImages();
    }
  }

  close(): void {
    if (this.uploading() || this.deletingId()) {
      return;
    }
    this.closed.emit();
  }

  loadImages(): void {
    const purchaseListId = this.purchaseList?.id;
    if (!purchaseListId) {
      this.images.set([]);
      return;
    }

    this.loadedPurchaseListId = purchaseListId;
    this.loading.set(true);
    this.purchaseImageService.findAll(purchaseListId).subscribe({
      next: (images) => {
        this.images.set(images);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('purchases.images.messages.loadError'))
        );
      }
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFiles.set(Array.from(input.files ?? []));
  }

  uploadSelected(): void {
    const purchaseListId = this.purchaseList?.id;
    const files = this.selectedFiles();

    if (!purchaseListId || files.length === 0) {
      this.toastService.warning(this.languageService.instant('purchases.images.messages.fileRequired'));
      return;
    }

    this.uploading.set(true);
    forkJoin(files.map((file) => this.purchaseImageService.upload(purchaseListId, file))).subscribe({
      next: () => {
        this.uploading.set(false);
        this.selectedFiles.set([]);
        if (this.fileInput?.nativeElement) {
          this.fileInput.nativeElement.value = '';
        }
        this.toastService.success(this.languageService.instant('purchases.images.messages.uploaded'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('purchases.images.messages.uploadError'))
        );
      }
    });
  }

  deleteImage(image: PurchaseImage): void {
    const purchaseListId = this.purchaseList?.id;
    if (!purchaseListId) {
      return;
    }

    const confirmed = window.confirm(
      this.languageService.instant('purchases.images.confirmDeleteMessage', { filename: image.originalFilename })
    );
    if (!confirmed) {
      return;
    }

    this.deletingId.set(image.id);
    this.purchaseImageService.delete(purchaseListId, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toastService.success(this.languageService.instant('purchases.images.messages.deleted'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('purchases.images.messages.deleteError'))
        );
      }
    });
  }

  openImage(image: PurchaseImage): void {
    if (!image.fileUrl) {
      return;
    }
    window.open(image.fileUrl, '_blank', 'noopener');
  }

  filesSummary(): string {
    const files = this.selectedFiles();
    if (files.length === 0) {
      return this.languageService.instant('purchases.images.upload.noFilesSelected');
    }
    if (files.length === 1) {
      return files[0].name;
    }
    return this.languageService.instant('purchases.images.upload.selectedFiles', { count: files.length });
  }

  purchaseListLabel(): string {
    const purchaseList = this.purchaseList;
    if (!purchaseList) {
      return '';
    }
    const parts = [purchaseList.purchaseDate, purchaseList.supplierName, purchaseList.propertyName].filter(Boolean);
    return parts.join(' · ');
  }

  formatSize(sizeBytes: number | null | undefined): string {
    if (sizeBytes == null) {
      return '—';
    }
    if (sizeBytes < 1024) {
      return `${sizeBytes} B`;
    }
    if (sizeBytes < 1024 * 1024) {
      return `${(sizeBytes / 1024).toFixed(1)} KB`;
    }
    return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private resetState(): void {
    this.images.set([]);
    this.selectedFiles.set([]);
    this.loading.set(false);
    this.uploading.set(false);
    this.deletingId.set(null);
    this.loadedPurchaseListId = null;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
