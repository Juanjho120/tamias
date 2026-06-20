# 14 — Product Box Models

Status: **In progress**

## Purpose

Add a TAMIAS module for 3D reconstruction of product boxes/packages. The goal is to let users register the physical dimensions of a rectangular box/prism, manage images for each face, render the box dynamically with Three.js, and process phone photos into clean textures using OpenCV Java.

This phase is intentionally placed **after 13A** because `13A` is already used by `68-ai-image-file-dashboard-tools-13a.md`.

## Current TAMIAS context

TAMIAS already has:

- Java 21 + Spring Boot 3 backend.
- Angular 19 standalone frontend.
- PostgreSQL + Flyway migrations.
- Private AWS S3 storage with presigned URLs.
- Organization-scoped multi-tenancy using `organization_id`.
- Existing hard-delete policy for entity image relationship tables.
- Existing S3 key strategy where `organizationId` is the first path segment inside the bucket.
- Existing static i18n files under `frontend/public/assets/i18n/en.json` and `frontend/public/assets/i18n/es.json`.

## Correct phase numbering

Do **not** call Product Box Models `13A` because 13A is already completed:

```text
13A — AI image/file dashboard tools
```

Use this sequence instead:

```text
14A — Product Box Models backend foundation
14B — Product Box Face Images
14C — Angular Product Box CRUD
14D — Three.js Product Box Viewer
14E — Product Box 3D Textures architecture/design
14F — Texture metadata + original upload
14G — OpenCV perspective correction backend
14H — Angular corner editor + processed texture preview
14I — Accept/retry/delete texture workflow
14J — Automatic contour detection and image enhancement
14K — Integration with Inventory/Purchases
14L — AI awareness for Product Box Models
```

Future phases after Product Box Models:

```text
15 — Reports
16 — Notifications and reminders
17 — Blueprint Analysis
```

## Completed subphases

### 14A — Product Box Models backend foundation

Status: **Completed**

Implemented:

- `product_box_models` table.
- Metadata CRUD endpoints.
- Organization scoping.
- Optional inventory item / purchase item association.
- Soft delete for the parent business entity.

### 14B — Product Box Face Images

Status: **Completed**

Implemented:

- `product_box_model_faces` table.
- One image per face: `front`, `back`, `left`, `right`, `top`, `bottom`.
- Private S3 storage with organization-first keys.
- Hard delete for face image rows and S3 objects.
- Presigned URLs in model detail responses.
- Parent delete removes face images before soft-deleting the parent model.

### 14C — Angular Product Box CRUD

Status: **Completed**

Implemented:

- Angular list/form UI for Product Box Models.
- Face upload controls.
- Static i18n keys under the app's JSON translation files.

### 14D — Three.js Product Box Viewer

Status: **Completed**

Implemented:

- Reusable Angular Three.js viewer component.
- Modal-based 3D preview.
- Texture per face using presigned URLs.
- Placeholder materials for missing faces.
- Orbit controls for rotate/zoom.
- Dynamic Three.js imports and WebGL cleanup.

### 14E — Product Box 3D Textures architecture/design

Status: **Completed / design ready**

Purpose:

- Define the OpenCV-based texture processing workflow.
- Extend the Product Box module with original photo, processed preview and accepted texture concepts.
- Keep all image deletes/replacements aligned with the current hard-delete S3 policy.
- Define automatic contour detection and basic image enhancement as a later subphase after the manual workflow is stable.

Documentation:

```text
docs/79-product-box-3d-textures-14e.md
```

### 14F — Texture metadata + original upload

Status: **Completed**

Implemented:

- Extended `product_box_model_faces` with original/processed/texture status metadata.
- Added upload endpoint for original phone photos.
- Stored original uploads in S3.
- Returned presigned URLs for original/processed/accepted images where applicable.
- No OpenCV processing yet.

Documentation:

```text
docs/80-product-box-texture-metadata-original-upload-14f.md
```

### 14G — OpenCV perspective correction backend

Status: **Completed**

Implemented:

- Added OpenCV Java dependency.
- Implemented perspective transform using four user-provided points.
- Generated processed texture with correct face aspect ratio.
- Stored processed output in S3.
- Persisted points, processed image metadata, processing status and errors.

Documentation:

```text
docs/81-product-box-opencv-perspective-correction-14g.md
```

### 14H — Angular corner editor + processed texture preview

Status: **Completed**

Implemented:

- Frontend corner editor with draggable four-point SVG overlay.
- Original photo upload integration.
- OpenCV process endpoint integration.
- Processed texture preview.
- CORS-consistent S3 image loading for previews/editor.
- Frontend point sanitization plus backend coordinate alignment fixes for phone photos.

Documentation:

```text
docs/82-product-box-angular-corner-editor-14h.md
```

### 14I — Accept/retry/delete texture workflow

Status: **Completed**

Implemented:

- Accept a processed texture as the active face texture used by Three.js.
- Keep retry flow safe: accepted texture remains active while a new original/processed draft is prepared.
- Delete original, processed and accepted texture objects from S3 before deleting the face row.
- Prevent draft cleanup from deleting the accepted texture when processed and active keys are the same.
- Hide the corner editor automatically after successful processing.

Documentation:

```text
docs/83-product-box-accept-retry-delete-texture-workflow-14i.md
```

## Upcoming subphases

### 14J — Automatic contour detection and image enhancement

Status: **Planned**

Expected scope:

- Detect rectangular face contour automatically when possible.
- Prefill corner points in the editor.
- Keep manual adjustment always available.
- Add conservative image enhancement such as basic brightness/contrast/shadow correction.

### 14K — Integration with Inventory/Purchases

Status: **Planned**

Expected scope:

- Show Product Box Models from inventory item and purchase item contexts.
- Allow creating a model from an existing item.
- Show indicators when an item already has a 3D box model.

### 14L — AI awareness for Product Box Models

Status: **Planned**

Expected scope:

- Let TAMI answer metadata-only questions about Product Box Models.
- Include dimensions, associated item, available faces and texture status.
- No image interpretation.
- No OCR/vision.

## Core data model decision

### `product_box_models`

This is the parent business entity and may use soft delete.

Current purpose:

- name,
- description,
- dimensions,
- unit,
- optional inventory item association,
- optional purchase item association,
- organization ownership.

Do not add `property_id` in the MVP unless a real use case appears. The box model primarily describes a product/item, not a property.

### `product_box_model_faces`

This is the canonical per-face texture/image table and must follow the hard-delete image policy.

Valid `face_name` values:

```text
front
back
left
right
top
bottom
```

The existing `s3_key` represents the active/accepted texture used by the Three.js viewer.

Texture processing subphases add original/processed metadata to this same table instead of introducing a separate canonical texture table.

## S3 key strategy

Use the existing organization-first strategy.

Accepted face texture key:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Original upload key:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/original/{filename}
```

Processed preview key:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/processed/{filename}
```

## Delete policy

Product box face images use hard delete.

When a face image/texture is deleted:

1. Delete S3 objects first.
2. If storage deletion succeeds, physically delete the face row.
3. If storage deletion fails, do not delete the database row.

When a parent model is deleted:

1. Delete all face S3 objects.
2. Physically delete face rows.
3. Soft-delete the parent product box model.

## Out of scope for Phase 14

- No `.glb`, `.gltf` or generated 3D file export.
- No backend 3D rendering.
- No AI visual interpretation of uploaded face images.
- No irregular package support.
- No stock-control integration.
- No Blueprint Analysis mixing.
