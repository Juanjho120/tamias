import { DatePipe } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
  computed,
  inject,
  signal
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PaymentImage } from '../../models/payment-image.model';
import { Payment } from '../../models/payment.model';
import { PaymentImageService } from '../../services/payment-image.service';

interface SelectedImagePreview {
  fileName: string;
  sizeBytes: number;
  url: string;
}

@Component({
  selector: 'app-payment-images-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ConfirmModalComponent],
  templateUrl: './payment-images-modal.component.html',
  styles: [
    `
      .payment-image-upload-preview {
        height: 72px;
        object-fit: cover;
        width: 96px;
      }

      .payment-image-preview {
        height: 180px;
        object-fit: cover;
        width: 100%;
      }
    `
  ]
})
export class PaymentImagesModalComponent implements OnChanges {
  @Input() open = false;
  @Input() payment: Payment | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() imagesChanged = new EventEmitter<void>();

  @ViewChild('fileInput') private readonly fileInput?: ElementRef<HTMLInputElement>;

  private readonly paymentImageService = inject(PaymentImageService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly images = signal<PaymentImage[]>([]);
  readonly selectedFiles = signal<File[]>([]);
  readonly selectedPreviews = signal<SelectedImagePreview[]>([]);
  readonly imageToDelete = signal<PaymentImage | null>(null);

  readonly deleteMessage = computed(() => {
    const image = this.imageToDelete();
    if (!image) {
      return '';
    }

    return this.languageService.instant('payments.images.confirmDeleteMessage', {
      filename: image.originalFilename
    });
  });

  private loadedPaymentId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.resetState();
      return;
    }

    const paymentId = this.payment?.id;
    const shouldLoad = !!paymentId && (changes['open'] || changes['payment']) && paymentId !== this.loadedPaymentId;

    if (shouldLoad) {
      this.loadImages();
    }
  }

  close(): void {
    if (this.uploading() || this.deletingId()) {
      return;
    }

    this.clearUploadSelection();
    this.closed.emit();
  }

  loadImages(): void {
    const paymentId = this.payment?.id;
    if (!paymentId) {
      this.images.set([]);
      return;
    }

    this.loadedPaymentId = paymentId;
    this.loading.set(true);

    this.paymentImageService.findAll(paymentId).subscribe({
      next: (images) => {
        this.images.set(images);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.images.messages.loadError'))
        );
      }
    });
  }

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);

    this.revokeSelectedPreviews();
    this.selectedFiles.set(files);
    this.selectedPreviews.set(
      files.map((file) => ({
        fileName: file.name,
        sizeBytes: file.size,
        url: URL.createObjectURL(file)
      }))
    );
  }

  clearUploadSelection(): void {
    this.selectedFiles.set([]);
    this.revokeSelectedPreviews();

    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }

  uploadSelected(): void {
    const paymentId = this.payment?.id;
    const files = this.selectedFiles();

    if (!paymentId || files.length === 0) {
      this.toastService.warning(this.languageService.instant('payments.images.messages.fileRequired'));
      return;
    }

    this.uploading.set(true);

    forkJoin(files.map((file) => this.paymentImageService.upload(paymentId, file))).subscribe({
      next: () => {
        this.uploading.set(false);
        this.clearUploadSelection();
        this.toastService.success(this.languageService.instant('payments.images.messages.uploaded'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.images.messages.uploadError'))
        );
      }
    });
  }

  requestDelete(image: PaymentImage): void {
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
    const paymentId = this.payment?.id;
    const image = this.imageToDelete();

    if (!paymentId || !image || this.deletingId()) {
      return;
    }

    this.deletingId.set(image.id);

    this.paymentImageService.delete(paymentId, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.imageToDelete.set(null);
        this.toastService.success(this.languageService.instant('payments.images.messages.deleted'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('payments.images.messages.deleteError'))
        );
      }
    });
  }

  openImage(image: PaymentImage): void {
    if (!image.fileUrl) {
      return;
    }

    window.open(image.fileUrl, '_blank', 'noopener');
  }

  filesSummary(): string {
    const files = this.selectedFiles();

    if (files.length === 0) {
      return this.languageService.instant('payments.images.upload.noFilesSelected');
    }

    if (files.length === 1) {
      return files[0].name;
    }

    return this.languageService.instant('payments.images.upload.selectedFiles', { count: files.length });
  }

  paymentLabel(): string {
    const payment = this.payment;
    if (!payment) {
      return '';
    }

    const parts = [payment.payDate, payment.name, payment.propertyName].filter(Boolean);
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

    return `${(sizeBytes / (1024 * 1024)).toFixed(2)} MB`;
  }

  private resetState(): void {
    this.images.set([]);
    this.selectedFiles.set([]);
    this.revokeSelectedPreviews();
    this.loading.set(false);
    this.uploading.set(false);
    this.deletingId.set(null);
    this.imageToDelete.set(null);
    this.loadedPaymentId = null;
  }

  private revokeSelectedPreviews(): void {
    for (const preview of this.selectedPreviews()) {
      URL.revokeObjectURL(preview.url);
    }

    this.selectedPreviews.set([]);
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
