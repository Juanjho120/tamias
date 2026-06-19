# 14D.1 — Auto texture crop and fit for Product Box Viewer

Status: Implemented in this phase.

## Goal

Improve the Three.js Product Box Viewer so face images are easier to use as textures when they come from phone photos or background-removal tools.

The viewer should automatically:

- load each face image into a browser canvas before creating the Three.js texture;
- detect the useful content area;
- crop transparent or border-connected background/padding;
- fit the result to the physical aspect ratio of the target face;
- create a `THREE.CanvasTexture` from the processed canvas.

## Scope

Included:

- Frontend-only texture preprocessing.
- No backend changes.
- No S3 changes.
- No migration.
- No destructive image edits.
- No re-upload of processed images.
- No `.glb`/`.gltf` generation.

The original S3 image remains unchanged. The crop/fit is applied only at render time inside the viewer.

## Face aspect ratios

The viewer uses the Product Box Model physical dimensions to compute each face ratio:

```text
front/back  = width / height
left/right  = depth / height
top/bottom  = width / depth
```

The preprocessed canvas is generated using the expected aspect ratio so the texture fits the face more naturally.

## Background handling

The algorithm handles two common cases:

1. **Transparent PNG/WebP with padding**
   - Detects visible pixels using the alpha channel.
   - Crops to the non-transparent area.

2. **Non-transparent image with border background**
   - Estimates the background color from image borders.
   - Detects the useful area by color distance from the border background.
   - Clears only border-connected background pixels.

The border-connected rule reduces the risk of deleting black labels/text inside the product image, because only pixels connected to the outside border are removed.

## Limitations

This is not a full image editor.

It does not correct:

- perspective distortion from angled phone photos;
- warped boxes;
- strong shadows that touch the product edge;
- cases where the product color is almost identical to the background;
- badly exported JPGs where the background was expected to be transparent.

For best results, users should still prefer:

```text
PNG or WebP with real transparency
```

instead of JPG when using background-removal tools.

## Future option

If automatic crop/fit is not enough, a later phase can add a manual face alignment editor with:

- crop box;
- zoom;
- pan;
- rotation;
- flip;
- saved crop metadata.

That is intentionally out of scope for 14D.1.

## Testing

Run:

```bash
cd frontend
npm run build
```

Manual tests:

1. Open Product Box Models.
2. Upload a transparent PNG/WebP with padding to one or more faces.
3. Open the 3D viewer.
4. Confirm the useful image content fills the face better.
5. Upload an image with dark/black border background.
6. Confirm border-connected background is reduced in the viewer.
7. Confirm missing faces still show placeholders.
8. Open/close the viewer several times to confirm WebGL cleanup still works.
