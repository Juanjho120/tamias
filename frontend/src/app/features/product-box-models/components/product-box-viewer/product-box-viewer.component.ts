import { NgClass } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  signal
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import {
  PRODUCT_BOX_FACE_NAMES,
  ProductBoxFaceName,
  ProductBoxModel,
  ProductBoxModelFace
} from '../../models/product-box-model.model';

type ThreeModule = typeof import('three');
type OrbitControlsModule = typeof import('three/examples/jsm/controls/OrbitControls.js');
type DisposableTexture = import('three').Texture;
type DisposableMaterial = import('three').Material;
type DisposableGeometry = import('three').BufferGeometry;
type OrbitControlsInstance = InstanceType<OrbitControlsModule['OrbitControls']>;

type RuntimeState = {
  renderer: import('three').WebGLRenderer;
  scene: import('three').Scene;
  camera: import('three').PerspectiveCamera;
  controls: OrbitControlsInstance;
  geometries: DisposableGeometry[];
  materials: DisposableMaterial[];
  textures: DisposableTexture[];
  animationFrameId: number;
  resizeObserver: ResizeObserver | null;
};

type RgbColor = {
  r: number;
  g: number;
  b: number;
};

type CropBounds = {
  minX: number;
  minY: number;
  maxX: number;
  maxY: number;
};

@Component({
  selector: 'app-product-box-viewer',
  standalone: true,
  imports: [NgClass, TranslatePipe],
  templateUrl: './product-box-viewer.component.html',
  styles: [
    `
      .product-box-viewer-shell {
        min-height: 420px;
      }

      .product-box-viewer-canvas-host {
        height: 420px;
        min-height: 320px;
      }

      @media (max-width: 768px) {
        .product-box-viewer-canvas-host {
          height: 340px;
        }
      }

      .product-box-viewer-loading,
      .product-box-viewer-error {
        inset: 0;
        z-index: 2;
      }
    `
  ]
})
export class ProductBoxViewerComponent implements AfterViewInit, OnChanges, OnDestroy {
  private static readonly MAX_PROCESSING_SIZE = 2048;
  private static readonly MAX_TEXTURE_SIZE = 1024;
  private static readonly MIN_TEXTURE_SIZE = 64;
  private static readonly ALPHA_THRESHOLD = 24;
  private static readonly BACKGROUND_TOLERANCE = 42;

  @Input() model: ProductBoxModel | null = null;

  @ViewChild('canvasHost') private readonly canvasHost?: ElementRef<HTMLDivElement>;

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly missingFaces = signal<ProductBoxFaceName[]>([]);

  private three: ThreeModule | null = null;
  private orbitControlsModule: OrbitControlsModule | null = null;
  private runtime: RuntimeState | null = null;
  private viewInitialized = false;
  private renderVersion = 0;

  ngAfterViewInit(): void {
    this.viewInitialized = true;
    this.renderModel();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['model'] && this.viewInitialized) {
      this.renderModel();
    }
  }

  ngOnDestroy(): void {
    this.disposeRuntime();
  }

  hasModel(): boolean {
    return !!this.model;
  }

  hasMissingFaces(): boolean {
    return this.missingFaces().length > 0;
  }

  faceTranslationKey(faceName: ProductBoxFaceName): string {
    return `productBoxModels.faces.names.${faceName}`;
  }

  dimensionsLabel(): string {
    if (!this.model) {
      return '—';
    }

    return `${this.model.width} × ${this.model.height} × ${this.model.depth} ${this.model.unit}`;
  }

  private async renderModel(): Promise<void> {
    const model = this.model;
    const host = this.canvasHost?.nativeElement;
    const version = ++this.renderVersion;

    this.disposeRuntime();
    this.errorMessage.set(null);

    if (!model || !host) {
      this.missingFaces.set([]);
      return;
    }

    this.loading.set(true);

    try {
      const three = await this.loadThree();

      if (version !== this.renderVersion) {
        return;
      }

      const runtime = await this.createRuntime(three, model, host);

      if (version !== this.renderVersion) {
        this.disposeRuntimeState(runtime);
        return;
      }

      this.runtime = runtime;
      this.animate(runtime);
      this.resize(runtime);
      this.loading.set(false);
    } catch (error) {
      console.error('[ProductBoxViewer] Failed to render product box model', error);
      this.loading.set(false);
      this.errorMessage.set('productBoxModels.viewer.renderError');
      this.disposeRuntime();
    }
  }

  private async loadThree(): Promise<ThreeModule> {
    if (!this.three) {
      this.three = await import('three');
    }

    if (!this.orbitControlsModule) {
      this.orbitControlsModule = await import('three/examples/jsm/controls/OrbitControls.js');
    }

    return this.three;
  }

  private async createRuntime(
    three: ThreeModule,
    model: ProductBoxModel,
    host: HTMLDivElement
  ): Promise<RuntimeState> {
    host.replaceChildren();

    const scene = new three.Scene();
    scene.background = new three.Color(0xf8f9fa);

    const width = Math.max(host.clientWidth, 1);
    const height = Math.max(host.clientHeight, 1);

    const camera = new three.PerspectiveCamera(45, width / height, 0.1, 1000);
    const normalizedDimensions = this.normalizedDimensions(model);
    const maxDimension = Math.max(normalizedDimensions.width, normalizedDimensions.height, normalizedDimensions.depth);
    camera.position.set(maxDimension * 1.4, maxDimension * 1.15, maxDimension * 1.75);

    const renderer = new three.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(width, height);
    host.appendChild(renderer.domElement);

    const geometry = new three.BoxGeometry(
      normalizedDimensions.width,
      normalizedDimensions.height,
      normalizedDimensions.depth
    );

    const { materials, textures } = await this.createMaterials(three, model);
    const mesh = new three.Mesh(geometry, materials);
    scene.add(mesh);

    const edges = new three.EdgesGeometry(geometry);
    const edgesMaterial = new three.LineBasicMaterial({ color: 0x6c757d });
    const edgeLines = new three.LineSegments(edges, edgesMaterial);
    scene.add(edgeLines);

    const ambientLight = new three.AmbientLight(0xffffff, 0.9);
    scene.add(ambientLight);

    const directionalLight = new three.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(5, 6, 8);
    scene.add(directionalLight);

    const OrbitControls = this.orbitControlsModule?.OrbitControls;
    if (!OrbitControls) {
      throw new Error('OrbitControls module was not loaded');
    }

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.08;
    controls.enablePan = false;
    controls.minDistance = Math.max(maxDimension * 0.9, 1);
    controls.maxDistance = Math.max(maxDimension * 5, 8);
    controls.target.set(0, 0, 0);
    controls.update();

    const resizeObserver = new ResizeObserver(() => this.resize(this.runtime));
    resizeObserver.observe(host);

    return {
      renderer,
      scene,
      camera,
      controls,
      geometries: [geometry, edges],
      materials: [...materials, edgesMaterial],
      textures,
      animationFrameId: 0,
      resizeObserver
    };
  }

  private async createMaterials(
    three: ThreeModule,
    model: ProductBoxModel
  ): Promise<{ materials: import('three').MeshBasicMaterial[]; textures: DisposableTexture[] }> {
    const textures: DisposableTexture[] = [];
    const missingFaces: ProductBoxFaceName[] = [];
    const materialFaceOrder: ProductBoxFaceName[] = ['right', 'left', 'top', 'bottom', 'front', 'back'];

    const materials = await Promise.all(
      materialFaceOrder.map(async (faceName) => {
        const face = model.faces?.[faceName] ?? null;
        const texture = await this.loadFaceTexture(three, model, faceName, face);

        if (texture) {
          textures.push(texture);
          return new three.MeshBasicMaterial({ map: texture, transparent: true, alphaTest: 0.01 });
        }

        missingFaces.push(faceName);
        return new three.MeshBasicMaterial({ color: this.placeholderColor(faceName) });
      })
    );

    this.missingFaces.set(
      missingFaces.sort((a, b) => PRODUCT_BOX_FACE_NAMES.indexOf(a) - PRODUCT_BOX_FACE_NAMES.indexOf(b))
    );

    return { materials, textures };
  }

  private async loadFaceTexture(
    three: ThreeModule,
    model: ProductBoxModel,
    faceName: ProductBoxFaceName,
    face: ProductBoxModelFace | null
  ): Promise<DisposableTexture | null> {
    if (!face?.imageUrl) {
      return null;
    }

    try {
      const image = await this.loadImage(face.imageUrl);
      const canvas = this.createFittedTextureCanvas(image, this.faceAspectRatio(model, faceName));
      const texture = new three.CanvasTexture(canvas);

      texture.colorSpace = three.SRGBColorSpace;
      texture.center.set(0.5, 0.5);

      if (face.rotationDegrees) {
        texture.rotation = three.MathUtils.degToRad(face.rotationDegrees);
      }

      if (face.flipHorizontal) {
        texture.repeat.x = -1;
        texture.offset.x = 1;
      }

      if (face.flipVertical) {
        texture.repeat.y = -1;
        texture.offset.y = 1;
      }

      texture.needsUpdate = true;
      return texture;
    } catch (error) {
      console.warn(`[ProductBoxViewer] Failed to load texture for face ${faceName}`, error);
      return null;
    }
  }

  private loadImage(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const image = new Image();
      image.crossOrigin = 'anonymous';
      image.onload = () => resolve(image);
      image.onerror = () => reject(new Error('Image could not be loaded'));
      image.src = url;
    });
  }

  private createFittedTextureCanvas(image: HTMLImageElement, targetAspectRatio: number): HTMLCanvasElement {
    const sourceCanvas = this.createSourceCanvas(image);
    const sourceContext = sourceCanvas.getContext('2d', { willReadFrequently: true });

    if (!sourceContext) {
      return sourceCanvas;
    }

    const imageData = sourceContext.getImageData(0, 0, sourceCanvas.width, sourceCanvas.height);
    const hasTransparency = this.hasMeaningfulTransparency(imageData);
    const backgroundColor = this.estimateBorderColor(imageData, sourceCanvas.width, sourceCanvas.height);
    const visibleBounds = this.findVisibleBounds(
      imageData,
      sourceCanvas.width,
      sourceCanvas.height,
      backgroundColor,
      hasTransparency
    );

    if (!hasTransparency) {
      this.clearBorderConnectedBackground(imageData, sourceCanvas.width, sourceCanvas.height, backgroundColor);
      sourceContext.putImageData(imageData, 0, 0);
    }

    const fittedBounds = this.expandBoundsToAspectRatio(
      visibleBounds,
      sourceCanvas.width,
      sourceCanvas.height,
      targetAspectRatio
    );
    return this.drawBoundsToAspectCanvas(sourceCanvas, fittedBounds, targetAspectRatio);
  }

  private createSourceCanvas(image: HTMLImageElement): HTMLCanvasElement {
    const naturalWidth = Math.max(image.naturalWidth || image.width || 1, 1);
    const naturalHeight = Math.max(image.naturalHeight || image.height || 1, 1);
    const scale = Math.min(1, ProductBoxViewerComponent.MAX_PROCESSING_SIZE / Math.max(naturalWidth, naturalHeight));
    const width = Math.max(Math.round(naturalWidth * scale), 1);
    const height = Math.max(Math.round(naturalHeight * scale), 1);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;

    const context = canvas.getContext('2d', { willReadFrequently: true });
    if (context) {
      context.imageSmoothingEnabled = true;
      context.imageSmoothingQuality = 'high';
      context.drawImage(image, 0, 0, width, height);
    }

    return canvas;
  }

  private hasMeaningfulTransparency(imageData: ImageData): boolean {
    const data = imageData.data;
    const totalPixels = Math.max(data.length / 4, 1);
    let transparentPixels = 0;

    for (let index = 3; index < data.length; index += 4) {
      if (data[index] < 250) {
        transparentPixels += 1;
      }
    }

    return transparentPixels / totalPixels > 0.001;
  }

  private estimateBorderColor(imageData: ImageData, width: number, height: number): RgbColor {
    const data = imageData.data;
    const step = Math.max(1, Math.floor(Math.max(width, height) / 256));
    let r = 0;
    let g = 0;
    let b = 0;
    let count = 0;

    const addPixel = (x: number, y: number): void => {
      const index = (y * width + x) * 4;
      const alpha = data[index + 3];

      if (alpha <= ProductBoxViewerComponent.ALPHA_THRESHOLD) {
        return;
      }

      r += data[index];
      g += data[index + 1];
      b += data[index + 2];
      count += 1;
    };

    for (let x = 0; x < width; x += step) {
      addPixel(x, 0);
      addPixel(x, height - 1);
    }

    for (let y = 0; y < height; y += step) {
      addPixel(0, y);
      addPixel(width - 1, y);
    }

    if (count === 0) {
      return { r: 0, g: 0, b: 0 };
    }

    return {
      r: r / count,
      g: g / count,
      b: b / count
    };
  }

  private findVisibleBounds(
    imageData: ImageData,
    width: number,
    height: number,
    backgroundColor: RgbColor,
    hasTransparency: boolean
  ): CropBounds {
    const data = imageData.data;
    let minX = width;
    let minY = height;
    let maxX = -1;
    let maxY = -1;

    for (let y = 0; y < height; y += 1) {
      for (let x = 0; x < width; x += 1) {
        const index = (y * width + x) * 4;
        const visible = hasTransparency
          ? data[index + 3] > ProductBoxViewerComponent.ALPHA_THRESHOLD
          : !this.isSimilarToBackground(data, index, backgroundColor);

        if (visible) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }

    if (maxX < minX || maxY < minY) {
      return { minX: 0, minY: 0, maxX: width - 1, maxY: height - 1 };
    }

    return { minX, minY, maxX, maxY };
  }

  private clearBorderConnectedBackground(imageData: ImageData, width: number, height: number, backgroundColor: RgbColor): void {
    const data = imageData.data;
    const visited = new Uint8Array(width * height);
    const queue: number[] = [];

    const enqueue = (x: number, y: number): void => {
      if (x < 0 || y < 0 || x >= width || y >= height) {
        return;
      }

      const pixelIndex = y * width + x;
      if (visited[pixelIndex]) {
        return;
      }

      const dataIndex = pixelIndex * 4;
      if (data[dataIndex + 3] <= ProductBoxViewerComponent.ALPHA_THRESHOLD) {
        visited[pixelIndex] = 1;
        return;
      }

      if (!this.isSimilarToBackground(data, dataIndex, backgroundColor)) {
        return;
      }

      visited[pixelIndex] = 1;
      queue.push(pixelIndex);
    };

    for (let x = 0; x < width; x += 1) {
      enqueue(x, 0);
      enqueue(x, height - 1);
    }

    for (let y = 0; y < height; y += 1) {
      enqueue(0, y);
      enqueue(width - 1, y);
    }

    for (let pointer = 0; pointer < queue.length; pointer += 1) {
      const pixelIndex = queue[pointer];
      const x = pixelIndex % width;
      const y = Math.floor(pixelIndex / width);
      const dataIndex = pixelIndex * 4;
      data[dataIndex + 3] = 0;

      enqueue(x + 1, y);
      enqueue(x - 1, y);
      enqueue(x, y + 1);
      enqueue(x, y - 1);
    }
  }

  private isSimilarToBackground(data: Uint8ClampedArray, index: number, backgroundColor: RgbColor): boolean {
    const redDiff = data[index] - backgroundColor.r;
    const greenDiff = data[index + 1] - backgroundColor.g;
    const blueDiff = data[index + 2] - backgroundColor.b;
    const toleranceSquared = ProductBoxViewerComponent.BACKGROUND_TOLERANCE ** 2;

    return redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff <= toleranceSquared;
  }

  private expandBoundsToAspectRatio(
    bounds: CropBounds,
    maxWidth: number,
    maxHeight: number,
    targetAspectRatio: number
  ): CropBounds {
    if (!Number.isFinite(targetAspectRatio) || targetAspectRatio <= 0) {
      return bounds;
    }

    const currentWidth = bounds.maxX - bounds.minX + 1;
    const currentHeight = bounds.maxY - bounds.minY + 1;
    const currentAspectRatio = currentWidth / currentHeight;
    const centerX = bounds.minX + currentWidth / 2;
    const centerY = bounds.minY + currentHeight / 2;
    let expandedWidth = currentWidth;
    let expandedHeight = currentHeight;

    if (currentAspectRatio < targetAspectRatio) {
      expandedWidth = currentHeight * targetAspectRatio;
    } else if (currentAspectRatio > targetAspectRatio) {
      expandedHeight = currentWidth / targetAspectRatio;
    }

    expandedWidth = Math.min(expandedWidth, maxWidth);
    expandedHeight = Math.min(expandedHeight, maxHeight);

    let minX = centerX - expandedWidth / 2;
    let maxX = centerX + expandedWidth / 2;
    let minY = centerY - expandedHeight / 2;
    let maxY = centerY + expandedHeight / 2;

    if (minX < 0) {
      maxX -= minX;
      minX = 0;
    }

    if (maxX > maxWidth) {
      minX -= maxX - maxWidth;
      maxX = maxWidth;
    }

    if (minY < 0) {
      maxY -= minY;
      minY = 0;
    }

    if (maxY > maxHeight) {
      minY -= maxY - maxHeight;
      maxY = maxHeight;
    }

    return {
      minX: Math.max(Math.floor(minX), 0),
      minY: Math.max(Math.floor(minY), 0),
      maxX: Math.min(Math.ceil(maxX) - 1, maxWidth - 1),
      maxY: Math.min(Math.ceil(maxY) - 1, maxHeight - 1)
    };
  }

  private drawBoundsToAspectCanvas(sourceCanvas: HTMLCanvasElement, bounds: CropBounds, targetAspectRatio: number): HTMLCanvasElement {
    const outputCanvas = document.createElement('canvas');
    const safeAspectRatio = Number.isFinite(targetAspectRatio) && targetAspectRatio > 0 ? targetAspectRatio : 1;

    if (safeAspectRatio >= 1) {
      outputCanvas.width = ProductBoxViewerComponent.MAX_TEXTURE_SIZE;
      outputCanvas.height = Math.max(
        Math.round(ProductBoxViewerComponent.MAX_TEXTURE_SIZE / safeAspectRatio),
        ProductBoxViewerComponent.MIN_TEXTURE_SIZE
      );
    } else {
      outputCanvas.height = ProductBoxViewerComponent.MAX_TEXTURE_SIZE;
      outputCanvas.width = Math.max(
        Math.round(ProductBoxViewerComponent.MAX_TEXTURE_SIZE * safeAspectRatio),
        ProductBoxViewerComponent.MIN_TEXTURE_SIZE
      );
    }

    const outputContext = outputCanvas.getContext('2d');
    if (!outputContext) {
      return sourceCanvas;
    }

    outputContext.clearRect(0, 0, outputCanvas.width, outputCanvas.height);
    outputContext.imageSmoothingEnabled = true;
    outputContext.imageSmoothingQuality = 'high';
    outputContext.drawImage(
      sourceCanvas,
      bounds.minX,
      bounds.minY,
      bounds.maxX - bounds.minX + 1,
      bounds.maxY - bounds.minY + 1,
      0,
      0,
      outputCanvas.width,
      outputCanvas.height
    );

    return outputCanvas;
  }

  private faceAspectRatio(model: ProductBoxModel, faceName: ProductBoxFaceName): number {
    const width = Math.max(Number(model.width) || 1, 1);
    const height = Math.max(Number(model.height) || 1, 1);
    const depth = Math.max(Number(model.depth) || 1, 1);

    switch (faceName) {
      case 'left':
      case 'right':
        return depth / height;
      case 'top':
      case 'bottom':
        return width / depth;
      case 'front':
      case 'back':
      default:
        return width / height;
    }
  }

  private normalizedDimensions(model: ProductBoxModel): { width: number; height: number; depth: number } {
    const width = Number(model.width) || 1;
    const height = Number(model.height) || 1;
    const depth = Number(model.depth) || 1;
    const maxDimension = Math.max(width, height, depth, 1);
    const targetMaxDimension = 4;
    const scale = targetMaxDimension / maxDimension;

    return {
      width: Math.max(width * scale, 0.1),
      height: Math.max(height * scale, 0.1),
      depth: Math.max(depth * scale, 0.1)
    };
  }

  private placeholderColor(faceName: ProductBoxFaceName): number {
    const colors: Record<ProductBoxFaceName, number> = {
      front: 0xe9ecef,
      back: 0xdee2e6,
      left: 0xf1f3f5,
      right: 0xe2e6ea,
      top: 0xf8f9fa,
      bottom: 0xdfe3e6
    };

    return colors[faceName];
  }

  private animate(runtime: RuntimeState): void {
    runtime.animationFrameId = window.requestAnimationFrame(() => this.animate(runtime));
    runtime.controls.update();
    runtime.renderer.render(runtime.scene, runtime.camera);
  }

  private resize(runtime: RuntimeState | null): void {
    if (!runtime || !this.canvasHost?.nativeElement) {
      return;
    }

    const host = this.canvasHost.nativeElement;
    const width = Math.max(host.clientWidth, 1);
    const height = Math.max(host.clientHeight, 1);

    runtime.camera.aspect = width / height;
    runtime.camera.updateProjectionMatrix();
    runtime.renderer.setSize(width, height);
  }

  private disposeRuntime(): void {
    if (!this.runtime) {
      return;
    }

    this.disposeRuntimeState(this.runtime);
    this.runtime = null;
  }

  private disposeRuntimeState(runtime: RuntimeState): void {
    window.cancelAnimationFrame(runtime.animationFrameId);
    runtime.resizeObserver?.disconnect();
    runtime.controls.dispose();
    runtime.geometries.forEach((geometry) => geometry.dispose());
    runtime.materials.forEach((material) => material.dispose());
    runtime.textures.forEach((texture) => texture.dispose());
    runtime.renderer.dispose();
    runtime.renderer.domElement.remove();
  }
}
