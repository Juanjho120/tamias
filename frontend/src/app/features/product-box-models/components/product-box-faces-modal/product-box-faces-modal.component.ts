import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ProductBoxTextureCornerEditorComponent } from '../product-box-texture-corner-editor/product-box-texture-corner-editor.component';
import {
  PRODUCT_BOX_FACE_NAMES,
  ProductBoxFaceName,
  ProductBoxModel,
  ProductBoxModelFace,
  ProductBoxRuntimeCapabilities,
  ProductBoxTexturePoint,
  ProductBoxTextureProcessRequest
} from '../../models/product-box-model.model';
import { ProductBoxModelService } from '../../services/product-box-model.service';

@Component({
  selector: 'app-product-box-faces-modal',
  standalone: true,
  imports: [DatePipe, TranslatePipe, ConfirmModalComponent, ProductBoxTextureCornerEditorComponent],
  templateUrl: './product-box-faces-modal.component.html',
  styles: [
    `
      .product-box-face-preview {
        height: 120px;
        object-fit: cover;
        width: 100%;
      }

      .product-box-texture-preview {
        max-height: 240px;
        object-fit: contain;
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
  readonly loadingCapabilities = signal(false);
  readonly runtimeCapabilities = signal<ProductBoxRuntimeCapabilities | null>(null);
  readonly uploadingFace = signal<ProductBoxFaceName | null>(null);
  readonly uploadingOriginalFace = signal<ProductBoxFaceName | null>(null);
  readonly detectingFace = signal<ProductBoxFaceName | null>(null);
  readonly processingFace = signal<ProductBoxFaceName | null>(null);
  readonly acceptingFace = signal<ProductBoxFaceName | null>(null);
  readonly enhancingFace = signal<ProductBoxFaceName | null>(null);
  readonly acceptingAiFace = signal<ProductBoxFaceName | null>(null);
  readonly discardingAiFace = signal<ProductBoxFaceName | null>(null);
  readonly deletingFace = signal<ProductBoxFaceName | null>(null);
  readonly editingTextureFace = signal<ProductBoxFaceName | null>(null);
  readonly faces = signal<Partial<Record<ProductBoxFaceName, ProductBoxModelFace>>>({});
  readonly faceToDelete = signal<ProductBoxFaceName | null>(null);
  readonly processPoints = signal<Partial<Record<ProductBoxFaceName, ProductBoxTextureProcessRequest>>>({});
  readonly freshEditorFaces = signal<Partial<Record<ProductBoxFaceName, boolean>>>({});

  readonly deleteMessage = computed(() => {
    const faceName = this.faceToDelete();
    if (!faceName) {
      return '';
    }

    const face = this.face(faceName);
    return this.languageService.instant('productBoxModels.faces.confirmDeleteMessage', {
      filename: face?.originalFilename ?? face?.originalUploadFilename ?? face?.processedFilename ?? faceName
    });
  });

  private loadedModelId: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.resetState();
      return;
    }

    const modelId = this.model?.id;
    const shouldLoad = !!modelId && (changes['open'] || changes['model']) && modelId !== this.loadedModelId;

    if (this.open && !this.runtimeCapabilities() && !this.loadingCapabilities()) {
      this.loadRuntimeCapabilities();
    }

    if (shouldLoad) {
      this.loadFaces();
    }
  }

  close(): void {
    if (this.isBusy()) {
      return;
    }

    this.closed.emit();
  }


  loadRuntimeCapabilities(): void {
    this.loadingCapabilities.set(true);

    this.productBoxModelService.getCapabilities().subscribe({
      next: (capabilities) => {
        this.runtimeCapabilities.set(capabilities);
        this.loadingCapabilities.set(false);
      },
      error: () => {
        this.loadingCapabilities.set(false);
      }
    });
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
        this.rehydrateSavedPoints(model.faces ?? {});
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

  isTextureEditorOpen(faceName: ProductBoxFaceName): boolean {
    return this.editingTextureFace() === faceName;
  }

  toggleTextureEditor(faceName: ProductBoxFaceName): void {
    if (this.editingTextureFace() === faceName) {
      this.editingTextureFace.set(null);
      return;
    }

    this.markFreshEditor(faceName, false);
    const existingPoints = this.processPoints()[faceName] ?? this.parsePointsJson(this.face(faceName)?.pointsJson ?? null);

    if (existingPoints) {
      this.processPoints.update((current) => ({ ...current, [faceName]: existingPoints }));
    }

    this.editingTextureFace.set(faceName);
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
    const request = currentFace?.imageKey
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

  uploadOriginalTexture(faceName: ProductBoxFaceName, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    const modelId = this.model?.id;

    if (!modelId || !file) {
      return;
    }

    this.uploadingOriginalFace.set(faceName);

    this.productBoxModelService.uploadOriginalTexture(modelId, faceName, file).subscribe({
      next: () => {
        this.uploadingOriginalFace.set(null);
        input.value = '';
        this.clearProcessPoints(faceName);
        this.markFreshEditor(faceName, true);
        this.editingTextureFace.set(faceName);
        this.toastService.success(this.languageService.instant('productBoxModels.textureEditor.messages.originalUploaded'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.uploadingOriginalFace.set(null);
        input.value = '';
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.textureEditor.messages.originalUploadError')));
      }
    });
  }

  detectContour(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const face = this.face(faceName);

    if (!modelId || !face?.originalImageUrl || this.detectingFace() || this.processingFace() || this.acceptingFace() || this.enhancingFace() || this.acceptingAiFace() || this.discardingAiFace()) {
      return;
    }

    if (!this.isOpenCvEnabled()) {
      this.toastService.error(this.openCvDisabledMessage());
      return;
    }

    this.detectingFace.set(faceName);

    this.productBoxModelService.detectTextureContour(modelId, faceName).subscribe({
      next: (response) => {
        this.detectingFace.set(null);

        if (response.detected && response.points) {
          const safePoints = this.sanitizeProcessPoints(face, response.points) ?? response.points;
          this.processPoints.update((current) => ({ ...current, [faceName]: safePoints }));
          this.markFreshEditor(faceName, false);
          this.editingTextureFace.set(faceName);
          this.toastService.success(this.languageService.instant('productBoxModels.textureEditor.messages.contourDetected'));
        } else {
          this.clearProcessPoints(faceName);
          this.markFreshEditor(faceName, true);
          this.editingTextureFace.set(faceName);
          this.toastService.error(response.message ?? this.languageService.instant('productBoxModels.textureEditor.messages.contourNotDetected'));
        }

        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.detectingFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.textureEditor.messages.contourDetectError')));
        this.loadFaces();
      }
    });
  }

  onTexturePointsChanged(faceName: ProductBoxFaceName, points: ProductBoxTextureProcessRequest): void {
    const face = this.face(faceName);
    const safePoints = face ? this.sanitizeProcessPoints(face, points) : points;

    if (!safePoints) {
      return;
    }

    const currentPoints = this.processPoints()[faceName];
    this.markFreshEditor(faceName, false);

    if (currentPoints && this.sameProcessPoints(currentPoints, safePoints)) {
      return;
    }

    this.processPoints.update((current) => ({ ...current, [faceName]: safePoints }));
  }

  processTexture(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const points = this.processPoints()[faceName];
    const face = this.face(faceName);

    if (!modelId || !face?.originalImageUrl || !points || this.processingFace() || this.acceptingFace() || this.enhancingFace() || this.acceptingAiFace() || this.discardingAiFace()) {
      return;
    }

    if (!this.isOpenCvEnabled()) {
      this.toastService.error(this.openCvDisabledMessage());
      return;
    }

    const safePoints = this.sanitizeProcessPoints(face, points);
    if (!safePoints) {
      this.toastService.error(this.languageService.instant('productBoxModels.textureEditor.messages.processError'));
      return;
    }

    const request: ProductBoxTextureProcessRequest = {
      ...safePoints,
      enhancementMode: face.enhancementMode ?? 'basic'
    };

    this.processPoints.update((current) => ({ ...current, [faceName]: request }));
    this.processingFace.set(faceName);

    this.productBoxModelService.processTexture(modelId, faceName, request).subscribe({
      next: () => {
        this.processingFace.set(null);
        if (this.editingTextureFace() === faceName) {
          this.editingTextureFace.set(null);
        }
        this.toastService.success(this.languageService.instant('productBoxModels.textureEditor.messages.processed'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.processingFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.textureEditor.messages.processError')));
        this.loadFaces();
      }
    });
  }

  acceptProcessedTexture(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const face = this.face(faceName);

    if (!modelId || !this.hasProcessedTexture(face) || this.acceptingFace() || this.enhancingFace() || this.acceptingAiFace() || this.discardingAiFace()) {
      return;
    }

    this.acceptingFace.set(faceName);

    this.productBoxModelService.acceptProcessedTexture(modelId, faceName).subscribe({
      next: () => {
        this.acceptingFace.set(null);
        if (this.editingTextureFace() === faceName) {
          this.editingTextureFace.set(null);
        }
        this.toastService.success(this.languageService.instant('productBoxModels.textureEditor.messages.accepted'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.acceptingFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.textureEditor.messages.acceptError')));
      }
    });
  }

  hasProcessedTexture(face: ProductBoxModelFace | null): boolean {
    return !!(
      face?.processedImageKey
      || face?.processedImageUrl
      || (face?.activeTextureSource === 'opencv_processed' && face?.imageUrl)
    );
  }

  canAcceptProcessedTexture(face: ProductBoxModelFace | null): boolean {
    return this.hasProcessedTexture(face) && face?.processedImageKey !== face?.imageKey;
  }

  canGenerateAiEnhancedTexture(face: ProductBoxModelFace | null): boolean {
    return this.isAiTextureEnhancementEnabled()
      && this.hasProcessedTexture(face)
      && !this.isAiEnhancedActive(face)
      && face?.aiEnhancementStatus !== 'PROCESSING';
  }

  canAcceptAiEnhancedTexture(face: ProductBoxModelFace | null): boolean {
    return !!face?.aiEnhancedImageUrl && face.aiEnhancedImageKey !== face.imageKey;
  }

  canDiscardAiEnhancedTexture(face: ProductBoxModelFace | null): boolean {
    return !!face?.aiEnhancedImageUrl && !this.isAiEnhancedActive(face);
  }

  isAiEnhancedActive(face: ProductBoxModelFace | null): boolean {
    return !!face?.aiEnhancedImageKey && face.aiEnhancedImageKey === face.imageKey;
  }

  generateAiEnhancedTexture(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const face = this.face(faceName);

    if (!modelId || this.enhancingFace() || this.acceptingFace() || this.acceptingAiFace() || this.deletingFace()) {
      return;
    }

    if (!this.isAiTextureEnhancementEnabled()) {
      this.toastService.error(this.aiTextureEnhancementDisabledMessage());
      return;
    }

    if (!this.canGenerateAiEnhancedTexture(face)) {
      return;
    }

    this.enhancingFace.set(faceName);

    this.productBoxModelService.generateAiEnhancedTexture(modelId, faceName).subscribe({
      next: () => {
        this.enhancingFace.set(null);
        this.toastService.success(this.languageService.instant('productBoxModels.aiTexture.messages.generated'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.enhancingFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.aiTexture.messages.generateError')));
        this.loadFaces();
      }
    });
  }

  acceptAiEnhancedTexture(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const face = this.face(faceName);

    if (!modelId || !this.canAcceptAiEnhancedTexture(face) || this.acceptingAiFace() || this.enhancingFace() || this.acceptingFace() || this.deletingFace()) {
      return;
    }

    this.acceptingAiFace.set(faceName);

    this.productBoxModelService.acceptAiEnhancedTexture(modelId, faceName).subscribe({
      next: () => {
        this.acceptingAiFace.set(null);
        this.toastService.success(this.languageService.instant('productBoxModels.aiTexture.messages.accepted'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.acceptingAiFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.aiTexture.messages.acceptError')));
      }
    });
  }

  discardAiEnhancedTexture(faceName: ProductBoxFaceName): void {
    const modelId = this.model?.id;
    const face = this.face(faceName);

    if (!modelId || !this.canDiscardAiEnhancedTexture(face) || this.discardingAiFace() || this.enhancingFace() || this.acceptingAiFace() || this.deletingFace()) {
      return;
    }

    this.discardingAiFace.set(faceName);

    this.productBoxModelService.discardAiEnhancedTexture(modelId, faceName).subscribe({
      next: () => {
        this.discardingAiFace.set(null);
        this.toastService.success(this.languageService.instant('productBoxModels.aiTexture.messages.discarded'));
        this.facesChanged.emit();
        this.loadFaces();
      },
      error: (error: unknown) => {
        this.discardingAiFace.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('productBoxModels.aiTexture.messages.discardError')));
      }
    });
  }

  retryTexture(faceName: ProductBoxFaceName): void {
    const face = this.face(faceName);

    if (!face?.originalImageUrl || this.processingFace() || this.acceptingFace() || this.enhancingFace() || this.acceptingAiFace() || this.discardingAiFace()) {
      return;
    }

    const existingPoints = this.processPoints()[faceName] ?? this.parsePointsJson(face.pointsJson ?? null);
    if (existingPoints) {
      const safePoints = this.sanitizeProcessPoints(face, existingPoints) ?? existingPoints;
      this.processPoints.update((current) => ({ ...current, [faceName]: safePoints }));
      this.markFreshEditor(faceName, false);
    } else {
      this.markFreshEditor(faceName, true);
    }

    this.editingTextureFace.set(faceName);
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

    this.productBoxModelService.deleteTexture(modelId, faceName).subscribe({
      next: () => {
        this.deletingFace.set(null);
        this.faceToDelete.set(null);
        this.processPoints.update((current) => {
          const next = { ...current };
          delete next[faceName];
          return next;
        });
        this.markFreshEditor(faceName, false);

        if (this.editingTextureFace() === faceName) {
          this.editingTextureFace.set(null);
        }

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

  openImage(imageUrl: string | null | undefined): void {
    if (!imageUrl) {
      return;
    }

    window.open(imageUrl, '_blank', 'noopener');
  }


  isOpenCvEnabled(): boolean {
    return this.runtimeCapabilities()?.opencvEnabled ?? true;
  }

  isAiTextureEnhancementEnabled(): boolean {
    return this.runtimeCapabilities()?.aiTextureEnhancementEnabled ?? true;
  }

  openCvDisabledMessage(): string {
    return this.runtimeCapabilities()?.opencvDisabledMessage
      ?? this.languageService.instant('productBoxModels.runtime.openCvDisabledMessage');
  }

  aiTextureEnhancementDisabledMessage(): string {
    return this.runtimeCapabilities()?.aiTextureEnhancementDisabledMessage
      ?? this.languageService.instant('productBoxModels.runtime.aiTextureDisabledMessage');
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

  initialPoints(faceName: ProductBoxFaceName): ProductBoxTextureProcessRequest | null {
    if (this.freshEditorFaces()[faceName]) {
      return null;
    }

    return this.processPoints()[faceName] ?? null;
  }

  private isBusy(): boolean {
    return !!(
      this.uploadingFace()
      || this.uploadingOriginalFace()
      || this.detectingFace()
      || this.processingFace()
      || this.acceptingFace()
      || this.enhancingFace()
      || this.acceptingAiFace()
      || this.discardingAiFace()
      || this.deletingFace()
    );
  }

  private clearProcessPoints(faceName: ProductBoxFaceName): void {
    this.processPoints.update((current) => {
      const next = { ...current };
      delete next[faceName];
      return next;
    });
  }

  private markFreshEditor(faceName: ProductBoxFaceName, fresh: boolean): void {
    this.freshEditorFaces.update((current) => ({ ...current, [faceName]: fresh }));
  }

  private rehydrateSavedPoints(faces: Partial<Record<ProductBoxFaceName, ProductBoxModelFace>>): void {
    const next: Partial<Record<ProductBoxFaceName, ProductBoxTextureProcessRequest>> = {};

    for (const faceName of this.faceNames) {
      const parsed = this.parsePointsJson(faces[faceName]?.pointsJson ?? null);
      if (parsed) {
        next[faceName] = parsed;
      }
    }

    this.processPoints.set(next);
  }

  private parsePointsJson(pointsJson: string | null): ProductBoxTextureProcessRequest | null {
    if (!pointsJson) {
      return null;
    }

    try {
      const parsed = JSON.parse(pointsJson) as ProductBoxTextureProcessRequest;
      if (this.isValidPointRequest(parsed)) {
        return parsed;
      }
    } catch {
      return null;
    }

    return null;
  }

  private isValidPointRequest(value: ProductBoxTextureProcessRequest): boolean {
    return ['topLeft', 'topRight', 'bottomRight', 'bottomLeft'].every((key) => {
      const point = value[key as keyof ProductBoxTextureProcessRequest] as ProductBoxTexturePoint;
      return Number.isFinite(point?.x) && Number.isFinite(point?.y);
    });
  }

  private sanitizeProcessPoints(
    face: ProductBoxModelFace,
    points: ProductBoxTextureProcessRequest
  ): ProductBoxTextureProcessRequest | null {
    const width = Math.trunc(face.originalWidthPx ?? 0);
    const height = Math.trunc(face.originalHeightPx ?? 0);

    if (width <= 0 || height <= 0) {
      return null;
    }

    const treatAsNormalized = this.arePointsNormalized(points);
    return {
      topLeft: this.sanitizePoint(points.topLeft, width, height, treatAsNormalized),
      topRight: this.sanitizePoint(points.topRight, width, height, treatAsNormalized),
      bottomRight: this.sanitizePoint(points.bottomRight, width, height, treatAsNormalized),
      bottomLeft: this.sanitizePoint(points.bottomLeft, width, height, treatAsNormalized),
      enhancementMode: points.enhancementMode ?? face.enhancementMode ?? 'basic'
    };
  }

  private arePointsNormalized(points: ProductBoxTextureProcessRequest): boolean {
    return ['topLeft', 'topRight', 'bottomRight', 'bottomLeft'].every((key) => {
      const point = points[key as keyof ProductBoxTextureProcessRequest] as ProductBoxTexturePoint;
      return point.x >= 0 && point.x <= 1 && point.y >= 0 && point.y <= 1;
    });
  }

  private sanitizePoint(
    point: ProductBoxTexturePoint,
    width: number,
    height: number,
    normalized: boolean
  ): ProductBoxTexturePoint {
    const maxX = Math.max(0, width - 1);
    const maxY = Math.max(0, height - 1);
    const rawX = normalized ? point.x * maxX : point.x;
    const rawY = normalized ? point.y * maxY : point.y;

    return {
      x: Math.trunc(this.clampNumber(Math.round(rawX), 0, maxX)),
      y: Math.trunc(this.clampNumber(Math.round(rawY), 0, maxY))
    };
  }

  private clampNumber(value: number, min: number, max: number): number {
    if (!Number.isFinite(value)) {
      return min;
    }

    return Math.max(min, Math.min(max, value));
  }


  private sameProcessPoints(first: ProductBoxTextureProcessRequest, second: ProductBoxTextureProcessRequest): boolean {
    return this.samePoint(first.topLeft, second.topLeft)
      && this.samePoint(first.topRight, second.topRight)
      && this.samePoint(first.bottomRight, second.bottomRight)
      && this.samePoint(first.bottomLeft, second.bottomLeft)
      && (first.enhancementMode ?? null) === (second.enhancementMode ?? null);
  }

  private samePoint(first: ProductBoxTexturePoint, second: ProductBoxTexturePoint): boolean {
    return Math.trunc(first.x) === Math.trunc(second.x) && Math.trunc(first.y) === Math.trunc(second.y);
  }

  private resetState(): void {
    this.faces.set({});
    this.runtimeCapabilities.set(null);
    this.processPoints.set({});
    this.freshEditorFaces.set({});
    this.loading.set(false);
    this.loadingCapabilities.set(false);
    this.uploadingFace.set(null);
    this.uploadingOriginalFace.set(null);
    this.detectingFace.set(null);
    this.processingFace.set(null);
    this.acceptingFace.set(null);
    this.enhancingFace.set(null);
    this.acceptingAiFace.set(null);
    this.discardingAiFace.set(null);
    this.deletingFace.set(null);
    this.editingTextureFace.set(null);
    this.faceToDelete.set(null);
    this.loadedModelId = null;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
