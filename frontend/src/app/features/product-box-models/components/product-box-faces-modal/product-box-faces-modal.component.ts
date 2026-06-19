import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import {
  PRODUCT_BOX_FACE_NAMES,
  ProductBoxFaceName,
  ProductBoxModel,
  ProductBoxModelFace
} from '../../models/product-box-model.model';
import { ProductBoxModelService } from '../../services/product-box-model.service';

@Component({
  selector: 'app-product-box-faces-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ConfirmModalComponent],
  templateUrl: './product-box-faces-modal.component.html',
  styles: [
    `
      .product-box-face-preview {
        height: 120px;
        object-fit: cover;
        width: 100%;
      }
    `
  ]
})
export class ProductBoxFacesModalComponent implements OnChanges {
  @Input() open = false;
  @Input() model: ProductBoxModel | null = null;

  @Output() closed = new EventEmitter<void>();
  @Output() facesChanged = new EventEmitter<void>();

  private readonly productBoxModelService = inject(ProductBoxModelService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly faceNames = PRODUCT_BOX_FACE_NAMES;
  readonly loading = signal(false);
  readonly uploadingFace = signal<ProductBoxFaceName | null>(null);
  readonly deletingFace = signal<ProductBoxFaceName | null>(null);
  readonly faces = signal<Partial<Record<ProductBoxFaceName, ProductBoxModelFace>>>({});
  readonly faceToDelete = signal<ProductBoxFaceName | null>(null);

  readonly deleteMessage = computed(() => {
    const faceName = this.faceToDelete();
    if (!faceName) {
      return '';
    }

    const face = this.face(faceName);
    return this.languageService.instant('productBoxModels.faces.confirmDeleteMessage', { filename: face?.originalFilename ?? faceName });
  });

  private loadedModelId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.resetState();
      return;
    }

    const modelId = this.model?.id;
    const shouldLoad = !!modelId && (changes['open'] || changes['model']) && modelId !== this.loadedModelId;

    if (shouldLoad) {
      this.loadFaces();
    }
  }

  close(): void {
    if (this.uploadingFace() || this.deletingFace()) {
      return;
    }

    this.closed.emit();
  }

  loadFaces(): void {
    const modelId = this.model?.id;
    if (!modelId) {
      this.faces.set({});
      return;
    }

    this.loadedModelId = modelId;
    this.loading.set(true);
    this.productBoxModelService.findById(modelId).subscribe({
      next: (model) => {
        this.faces.set(model.faces ?? {});
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.faces.messages.loadError')));
      }
    });
  }

  face(faceName: ProductBoxFaceName): ProductBoxModelFace | null {
    return this.faces()[faceName] ?? null;
  }

  uploadFace(faceName: ProductBoxFaceName, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    const modelId = this.model?.id;

    if (!modelId || !file) {
      return;
    }

    this.uploadingFace.set(faceName);
    const currentFace = this.face(faceName);
    const request = currentFace
      ? this.productBoxModelService.replaceFace(modelId, faceName, file)
      : this.productBoxModelService.uploadFace(modelId, faceName, file);

    request.subscribe({
      next: () => {
        this.uploadingFace.set(null);
        input.value = '';
        this.toastService.success(this.languageService.instant('productBoxModels.faces.messages.saved'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.uploadingFace.set(null);
        input.value = '';
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.faces.messages.saveError')));
      }
    });
  }

  requestDelete(faceName: ProductBoxFaceName): void {
    if (this.deletingFace()) {
      return;
    }

    this.faceToDelete.set(faceName);
  }

  cancelDelete(): void {
    if (this.deletingFace()) {
      return;
    }

    this.faceToDelete.set(null);
  }

  confirmDelete(): void {
    const modelId = this.model?.id;
    const faceName = this.faceToDelete();

    if (!modelId || !faceName || this.deletingFace()) {
      return;
    }

    this.deletingFace.set(faceName);
    this.productBoxModelService.deleteFace(modelId, faceName).subscribe({
      next: () => {
        this.deletingFace.set(null);
        this.faceToDelete.set(null);
        this.toastService.success(this.languageService.instant('productBoxModels.faces.messages.deleted'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.deletingFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.faces.messages.deleteError')));
      }
    });
  }

  openImage(face: ProductBoxModelFace): void {
    if (!face.imageUrl) {
      return;
    }

    window.open(face.imageUrl, '_blank', 'noopener');
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
    this.faces.set({});
    this.loading.set(false);
    this.uploadingFace.set(null);
    this.deletingFace.set(null);
    this.faceToDelete.set(null);
    this.loadedModelId = null;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
