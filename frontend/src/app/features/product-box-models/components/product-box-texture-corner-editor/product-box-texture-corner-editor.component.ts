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
  signal
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProductBoxTexturePoint, ProductBoxTextureProcessRequest } from '../../models/product-box-model.model';

type CornerName = keyof ProductBoxTextureProcessRequest;
type NormalizedCorners = Record<CornerName, ProductBoxTexturePoint>;

@Component({
  selector: 'app-product-box-texture-corner-editor',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './product-box-texture-corner-editor.component.html',
  styles: [
    `
      .corner-editor-frame {
        display: inline-block;
        max-width: 100%;
        position: relative;
        user-select: none;
      }

      .corner-editor-image {
        display: block;
        max-height: 520px;
        max-width: 100%;
      }

      .corner-editor-overlay {
        height: 100%;
        inset: 0;
        position: absolute;
        touch-action: none;
        width: 100%;
      }

      .corner-editor-point {
        cursor: grab;
      }

      .corner-editor-point:active {
        cursor: grabbing;
      }
    `
  ]
})
export class ProductBoxTextureCornerEditorComponent implements OnChanges {
  @Input() imageUrl: string | null = null;
  @Input() imageWidth: number | null = null;
  @Input() imageHeight: number | null = null;
  @Input() disabled = false;
  @Input() initialPoints: ProductBoxTextureProcessRequest | null = null;

  @Output() pointsChanged = new EventEmitter<ProductBoxTextureProcessRequest>();

  @ViewChild('imageElement') private readonly imageElement?: ElementRef<HTMLImageElement>;

  readonly imageReady = signal(false);
  readonly draggingCorner = signal<CornerName | null>(null);
  readonly normalizedCorners = signal<NormalizedCorners>(this.defaultCorners());

  readonly polygonPoints = computed(() => {
    const corners = this.normalizedCorners();

    return [corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft]
      .map((point) => `${point.x * 100},${point.y * 100}`)
      .join(' ');
  });

  readonly cornerEntries = computed(() => {
    const corners = this.normalizedCorners();

    return [
      { name: 'topLeft' as CornerName, point: corners.topLeft },
      { name: 'topRight' as CornerName, point: corners.topRight },
      { name: 'bottomRight' as CornerName, point: corners.bottomRight },
      { name: 'bottomLeft' as CornerName, point: corners.bottomLeft }
    ];
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['imageUrl']) {
      this.imageReady.set(false);
    }

    if (changes['initialPoints'] && this.initialPoints) {
      this.normalizedCorners.set(this.toNormalizedCorners(this.initialPoints));
      this.emitRealPoints();
      return;
    }

    if (changes['imageUrl'] && this.imageUrl) {
      this.normalizedCorners.set(this.defaultCorners());
    }
  }

  onImageLoad(): void {
    this.imageReady.set(true);

    if (this.initialPoints) {
      this.normalizedCorners.set(this.toNormalizedCorners(this.initialPoints));
    }

    this.emitRealPoints();
  }

  resetPoints(): void {
    this.normalizedCorners.set(this.defaultCorners());
    this.emitRealPoints();
  }

  startDrag(corner: CornerName, event: PointerEvent): void {
    if (this.disabled) {
      return;
    }

    event.preventDefault();
    this.draggingCorner.set(corner);
    (event.target as Element).setPointerCapture?.(event.pointerId);
    this.updateCornerFromPointer(corner, event);
  }

  moveDrag(event: PointerEvent): void {
    const corner = this.draggingCorner();

    if (!corner || this.disabled) {
      return;
    }

    event.preventDefault();
    this.updateCornerFromPointer(corner, event);
  }

  endDrag(event?: PointerEvent): void {
    const corner = this.draggingCorner();

    if (event && corner) {
      (event.target as Element).releasePointerCapture?.(event.pointerId);
    }

    this.draggingCorner.set(null);
    this.emitRealPoints();
  }

  private updateCornerFromPointer(corner: CornerName, event: PointerEvent): void {
    const rect = this.imageElement?.nativeElement.getBoundingClientRect();

    if (!rect || rect.width <= 0 || rect.height <= 0) {
      return;
    }

    const x = this.clamp((event.clientX - rect.left) / rect.width, 0, 1);
    const y = this.clamp((event.clientY - rect.top) / rect.height, 0, 1);

    this.normalizedCorners.update((current) => ({
      ...current,
      [corner]: { x, y }
    }));

    this.emitRealPoints();
  }

  private emitRealPoints(): void {
    if (!this.imageReady() && !this.imageWidth && !this.imageHeight) {
      return;
    }

    const width = this.imageWidth ?? this.imageElement?.nativeElement.naturalWidth ?? 0;
    const height = this.imageHeight ?? this.imageElement?.nativeElement.naturalHeight ?? 0;

    if (width <= 0 || height <= 0) {
      return;
    }

    const corners = this.normalizedCorners();

    this.pointsChanged.emit({
      topLeft: this.toRealPoint(corners.topLeft, width, height),
      topRight: this.toRealPoint(corners.topRight, width, height),
      bottomRight: this.toRealPoint(corners.bottomRight, width, height),
      bottomLeft: this.toRealPoint(corners.bottomLeft, width, height)
    });
  }

  private toNormalizedCorners(points: ProductBoxTextureProcessRequest): NormalizedCorners {
    const width = this.imageWidth ?? this.imageElement?.nativeElement.naturalWidth ?? 0;
    const height = this.imageHeight ?? this.imageElement?.nativeElement.naturalHeight ?? 0;

    if (width <= 0 || height <= 0) {
      return this.defaultCorners();
    }

    return {
      topLeft: this.toNormalizedPoint(points.topLeft, width, height),
      topRight: this.toNormalizedPoint(points.topRight, width, height),
      bottomRight: this.toNormalizedPoint(points.bottomRight, width, height),
      bottomLeft: this.toNormalizedPoint(points.bottomLeft, width, height)
    };
  }

  private toNormalizedPoint(point: ProductBoxTexturePoint, width: number, height: number): ProductBoxTexturePoint {
    return {
      x: this.clamp(point.x / Math.max(1, Math.trunc(width) - 1), 0, 1),
      y: this.clamp(point.y / Math.max(1, Math.trunc(height) - 1), 0, 1)
    };
  }

  private toRealPoint(point: ProductBoxTexturePoint, width: number, height: number): ProductBoxTexturePoint {
    const maxX = Math.max(0, Math.trunc(width) - 1);
    const maxY = Math.max(0, Math.trunc(height) - 1);

    return {
      x: Math.trunc(this.clamp(Math.round(point.x * maxX), 0, maxX)),
      y: Math.trunc(this.clamp(Math.round(point.y * maxY), 0, maxY))
    };
  }

  private defaultCorners(): NormalizedCorners {
    return {
      topLeft: { x: 0.08, y: 0.08 },
      topRight: { x: 0.92, y: 0.08 },
      bottomRight: { x: 0.92, y: 0.92 },
      bottomLeft: { x: 0.08, y: 0.92 }
    };
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }
}
