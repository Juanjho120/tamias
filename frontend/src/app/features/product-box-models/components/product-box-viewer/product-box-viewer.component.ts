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

type RuntimeState = {
  renderer: import('three').WebGLRenderer;
  scene: import('three').Scene;
  camera: import('three').PerspectiveCamera;
  controls: InstanceType<OrbitControlsModule['OrbitControls']>;
  geometries: DisposableGeometry[];
  materials: DisposableMaterial[];
  textures: DisposableTexture[];
  animationFrameId: number;
  resizeObserver: ResizeObserver | null;
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

    const materialFaceOrder: ProductBoxFaceName[] = [
      'right',
      'left',
      'top',
      'bottom',
      'front',
      'back'
    ];

    const materials = await Promise.all(
      materialFaceOrder.map(async (faceName) => {
        const face = model.faces?.[faceName] ?? null;
        const texture = await this.loadFaceTexture(three, face);

        if (texture) {
          textures.push(texture);
          return new three.MeshBasicMaterial({ map: texture });
        }

        missingFaces.push(faceName);
        return new three.MeshBasicMaterial({ color: this.placeholderColor(faceName) });
      })
    );

    this.missingFaces.set(missingFaces.sort((a, b) => PRODUCT_BOX_FACE_NAMES.indexOf(a) - PRODUCT_BOX_FACE_NAMES.indexOf(b)));

    return { materials, textures };
  }

  private async loadFaceTexture(
    three: ThreeModule,
    face: ProductBoxModelFace | null
  ): Promise<DisposableTexture | null> {
    if (!face?.imageUrl) {
      return null;
    }

    return new Promise((resolve) => {
      const loader = new three.TextureLoader();
      loader.setCrossOrigin('anonymous');
      loader.load(
        face.imageUrl ?? '',
        (texture) => {
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

          resolve(texture);
        },
        undefined,
        () => resolve(null)
      );
    });
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
