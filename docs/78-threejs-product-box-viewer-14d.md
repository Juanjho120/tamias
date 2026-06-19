# 14D — Three.js Product Box Viewer

Status: Completed.

## Goal

Add a reusable Angular viewer that reconstructs Product Box Models as simple rectangular prisms in the browser using Three.js.

This phase uses the metadata and face image presigned URLs delivered by 14A/14B/14C:

- `width`, `height`, `depth`, `unit` from `product_box_models`.
- `front`, `back`, `left`, `right`, `top`, `bottom` face images from `product_box_model_faces`.
- Private S3 objects exposed through existing backend presigned URLs.

## Scope

Included:

- Install `three` and `@types/three` in the Angular frontend.
- Add `ProductBoxViewerComponent`.
- Add `ProductBoxViewerModalComponent`.
- Add a `View 3D` action in the Product Box Models list.
- Render a Three.js `BoxGeometry` using one material per face.
- Preserve the correct Three.js `BoxGeometry` material order:
  - right
  - left
  - top
  - bottom
  - front
  - back
- Add OrbitControls for rotate/zoom interaction.
- Show placeholder materials for missing faces.
- Normalize the render size while preserving physical proportions.
- Dispose renderer, geometries, materials, textures, controls and animation frame on destroy.
- Use dynamic imports so Three.js does not inflate the initial bundle unnecessarily.

Not included:

- No backend changes.
- No migrations.
- No S3 changes.
- No `.glb` or `.gltf` export.
- No OCR/vision.
- No AI tools.
- No inventory/purchase detail integration yet; that remains in 14E.

## Frontend architecture

New components:

```text
frontend/src/app/features/product-box-models/components/product-box-viewer/
frontend/src/app/features/product-box-models/components/product-box-viewer-modal/
```

The viewer receives a full `ProductBoxModel` object. It does not fetch data by itself. The page loads the model detail first so the viewer receives the latest presigned URLs.

## Three.js loading strategy

Three.js and OrbitControls are loaded dynamically inside the viewer component:

```ts
await import('three');
await import('three/examples/jsm/controls/OrbitControls.js');
```

This keeps Product Box Models isolated from the initial Angular bundle as much as possible.

## Face rendering

The viewer maps TAMIAS faces to Three.js material order as follows:

```text
right  -> material[0]
left   -> material[1]
top    -> material[2]
bottom -> material[3]
front  -> material[4]
back   -> material[5]
```

If a face image is missing, the viewer uses a neutral placeholder material and shows a small missing-faces notice.

## Texture preprocessing added in 14D.1

After 14D.1, the viewer does not pass the raw image directly to Three.js. Instead, it:

1. loads the face image into an HTML canvas;
2. detects useful image content using alpha transparency or border-background detection;
3. crops padding/background;
4. fits the crop to the target face aspect ratio;
5. creates a `THREE.CanvasTexture` from the processed canvas.

This improves images exported from background-removal tools, especially transparent PNG/WebP files with large padding or non-transparent images with dark border backgrounds.

See `79-product-box-texture-crop-fit-14d1.md` for details.

## Resource cleanup

`ProductBoxViewerComponent` must clean up:

- animation frame
- ResizeObserver
- OrbitControls
- BoxGeometry
- edge geometry
- face materials
- textures
- WebGLRenderer
- canvas element

This prevents WebGL memory leaks when opening/closing the modal multiple times.

## Deployment notes

No Docker, Vercel or Render configuration changes are required.

The frontend dependency files must be updated after installing packages:

```bash
cd frontend
npm install three
npm install -D @types/three
```

Then run:

```bash
npm run build
```

If a production bundle budget warning appears later, prefer lazy route/chunk improvements before increasing Angular budgets.

## Testing

Run:

```bash
cd frontend
npm run build
```

Manual tests:

1. Open Product Box Models.
2. Create or select a model with dimensions.
3. Upload at least one face image from the Faces modal.
4. Click `View 3D`.
5. Confirm the box renders.
6. Drag to rotate.
7. Use mouse wheel / trackpad to zoom.
8. Confirm missing faces show placeholders.
9. Upload/replace face images and reopen the viewer.
10. Open and close the viewer several times to confirm no UI freezing or repeated canvas stacking.
11. Upload a transparent PNG/WebP with padding and confirm 14D.1 crop/fit improves the texture placement.
