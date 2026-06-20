# 14H — Angular Corner Editor + Processed Texture Preview

Status: **Implemented**

## Purpose

Add the frontend MVP for Product Box 3D texture processing. This phase lets the user upload an original photo for a Product Box face, mark the four real corners of that face, send those pixel coordinates to the OpenCV backend from 14G, and preview the processed texture returned by the backend.

## Scope

This phase is frontend-only.

Included:

- Extend Product Box frontend models with texture metadata returned by the backend.
- Extend `ProductBoxModelService` with:
  - upload original texture endpoint.
  - process texture endpoint.
- Add a reusable corner editor component.
- Integrate the editor into the existing Product Box faces modal.
- Show active texture, original photo, and processed texture side by side.
- Send corner coordinates as real original-image pixels.
- Keep the processed result as preview only.

Not included:

- Accepting a processed texture as the active Three.js texture.
- Retry/delete workflow refinements beyond the existing hard-delete face behavior.
- Automatic contour detection.
- Automatic illumination enhancement.
- Backend changes.
- AI awareness.

## Frontend components

New component:

```text
frontend/src/app/features/product-box-models/components/product-box-texture-corner-editor
```

Updated component:

```text
frontend/src/app/features/product-box-models/components/product-box-faces-modal
```

The corner editor uses the original uploaded image and an SVG overlay with four draggable points:

- `topLeft`
- `topRight`
- `bottomRight`
- `bottomLeft`

The editor stores drag positions internally as normalized values for responsive display, but emits real image pixel coordinates using the original image width and height returned by the backend.

## Backend endpoints used

Original upload:

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/original
```

Process texture:

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/process
```

Request body:

```json
{
  "topLeft": { "x": 123, "y": 456 },
  "topRight": { "x": 789, "y": 450 },
  "bottomRight": { "x": 800, "y": 1200 },
  "bottomLeft": { "x": 110, "y": 1210 }
}
```

Coordinates are real pixels from the original image.

## UI behavior

For each face, the modal now shows:

1. Active texture — the currently accepted texture used by the Three.js viewer.
2. Original photo — the uploaded source image to adjust.
3. Processed texture — the OpenCV output preview.

The user can:

- upload/replace the original photo,
- open the corner editor,
- drag four corner points,
- reset points,
- process the texture,
- preview the processed result.

The processed texture does not become active in this phase. That happens in 14I.

## i18n

Translations are provided as addition files:

```text
frontend/public/assets/i18n/es.14h-product-box-texture-editor.additions.json
frontend/public/assets/i18n/en.14h-product-box-texture-editor.additions.json
```

These blocks must be merged into the real files:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

No new translation service is introduced.

## Acceptance criteria

- User can upload an original photo for a Product Box face.
- User can open an editor for that face.
- User can drag four corners over the image.
- Frontend sends real pixel coordinates to the backend.
- Backend returns processed texture metadata and presigned URL.
- Frontend shows the processed texture preview.
- Existing active face upload/delete behavior still works.
- No native browser confirm dialogs are introduced.
- No backend changes are required in this phase.

## Next phase

`14I — Accept/retry/delete texture workflow`

That phase will promote a processed texture to the active `s3_key` used by the Three.js viewer, handle retry semantics, and refine delete behavior for original/processed/accepted texture assets.
