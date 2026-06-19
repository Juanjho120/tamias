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
import { ReservationImage } from '../../models/reservation-image.model';
import { ReservationSummary } from '../../models/reservation.model';
import { ReservationImageService } from '../../services/reservation-image.service';

interface SelectedImagePreview {
  fileName: string;
  sizeBytes: number;
  url: string;
}

@Component({
  selector: 'app-reservation-images-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ConfirmModalComponent],
  templateUrl: './reservation-images-modal.component.html',
  styles: [
    `
      .reservation-image-upload-preview {
        height: 72px;
        object-fit: cover;
        width: 96px;
      }
    `
  ]
})
export class ReservationImagesModalComponent implements OnChanges {
  @Input() open = false;
  @Input() reservation: ReservationSummary | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() imagesChanged = new EventEmitter<void>();

  @ViewChild('fileInput') private readonly fileInput?: ElementRef<HTMLInputElement>;

  private readonly reservationImageService = inject(ReservationImageService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly images = signal<ReservationImage[]>([]);
  readonly selectedFiles = signal<File[]>([]);
  readonly selectedPreviews = signal<SelectedImagePreview[]>([]);
  readonly imageToDelete = signal<ReservationImage | null>(null);

  readonly deleteMessage = computed(() => {
    const image = this.imageToDelete();
    if (!image) {
      return '';
    }

    return this.languageService.instant('reservations.images.confirmDeleteMessage', {
      filename: image.originalFilename
    });
  });

  private loadedReservationId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.resetState();
      return;
    }

    const reservationId = this.reservation?.id;
    const shouldLoad =
      !!reservationId &&
      (changes['open'] || changes['reservation']) &&
      reservationId !== this.loadedReservationId;

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
    const reservationId = this.reservation?.id;

    if (!reservationId) {
      this.images.set([]);
      return;
    }

    this.loadedReservationId = reservationId;
    this.loading.set(true);
    this.reservationImageService.findAll(reservationId).subscribe({
      next: (images) => {
        this.images.set(images);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('reservations.images.messages.loadError'))
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
    const reservationId = this.reservation?.id;
    const files = this.selectedFiles();

    if (!reservationId || files.length === 0) {
      this.toastService.warning(this.languageService.instant('reservations.images.messages.fileRequired'));
      return;
    }

    this.uploading.set(true);
    forkJoin(files.map((file) => this.reservationImageService.upload(reservationId, file))).subscribe({
      next: () => {
        this.uploading.set(false);
        this.clearUploadSelection();
        this.toastService.success(this.languageService.instant('reservations.images.messages.uploaded'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('reservations.images.messages.uploadError'))
        );
      }
    });
  }

  requestDelete(image: ReservationImage): void {
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
    const reservationId = this.reservation?.id;
    const image = this.imageToDelete();

    if (!reservationId || !image || this.deletingId()) {
      return;
    }

    this.deletingId.set(image.id);
    this.reservationImageService.delete(reservationId, image.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.imageToDelete.set(null);
        this.toastService.success(this.languageService.instant('reservations.images.messages.deleted'));
        this.imagesChanged.emit();
        this.loadImages();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(
          this.extractErrorMessage(error, this.languageService.instant('reservations.images.messages.deleteError'))
        );
      }
    });
  }

  openImage(image: ReservationImage): void {
    if (!image.fileUrl) {
      return;
    }

    window.open(image.fileUrl, '_blank', 'noopener');
  }

  filesSummary(): string {
    const files = this.selectedFiles();

    if (files.length === 0) {
      return this.languageService.instant('reservations.images.upload.noFilesSelected');
    }

    if (files.length === 1) {
      return files[0].name;
    }

    return this.languageService.instant('reservations.images.upload.selectedFiles', {
      count: files.length
    });
  }

  reservationLabel(): string {
    const reservation = this.reservation;

    if (!reservation) {
      return '';
    }

    const parts = [reservation.reservationCode, reservation.propertyName, `${reservation.checkIn} → ${reservation.checkOut}`].filter(
      Boolean
    );

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
    this.loadedReservationId = null;
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
