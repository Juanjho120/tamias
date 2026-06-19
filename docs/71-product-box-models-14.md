# 14 — Product Box Models

Status: **In progress**

## Purpose

Add a new TAMIAS module for simple 3D reconstruction of product boxes/packages. The goal is to let users register the physical dimensions of a rectangular box/prism and upload one image for each face so the frontend can render the box dynamically with Three.js.

This phase is intentionally placed **after 13A** because `13A` is already used by `68-ai-image-file-dashboard-tools-13a.md`. The product box work must therefore start at **Phase 14**.

## Context

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

Do **not** call this work `13A` because 13A is already completed:

```text
13A — AI image/file dashboard tools
```

Use this sequence instead:

```text
14A — Product Box Models backend foundation
14B — Product Box Face Images
14C — Angular Product Box CRUD
14D — Three.js Product Box Viewer
14D.1 — Auto texture crop and fit
14E — Integration with Inventory/Purchases
14F — AI awareness for Product Box Models
```

Future phases after Product Box Models:

```text
15 — Reports
16 — Notifications and reminders
17 — Blueprint Analysis
```

## MVP scope

The MVP should support rectangular boxes only.

Included:

- Create a product box model.
- Associate it optionally with an inventory item and/or purchase item.
- Store name, description, dimensions and unit.
- Upload one image per face.
- Render the box dynamically in Angular using Three.js.
- Use private S3 objects and presigned URLs.
- Delete face images from S3 when replaced or deleted.
- Delete all face images from S3 when deleting a model.
- Keep all queries organization-scoped.

Not included in the MVP:

- No `.glb`, `.gltf` or generated 3D file export.
- No backend 3D rendering.
- No AI visual interpretation of uploaded face images.
- No OCR or vision analysis in this phase.
- No stock-control integration.
- No advanced mesh editing.
- No support for irregular packages.

## Data model decision

### `product_box_models`

This is the parent business entity.

Recommended columns:

```text
id UUID PK
organization_id UUID NOT NULL
inventory_item_id UUID NULL
purchase_item_id UUID NULL
name VARCHAR(255) NOT NULL
description TEXT NULL
width NUMERIC(10,2) NOT NULL
height NUMERIC(10,2) NOT NULL
depth NUMERIC(10,2) NOT NULL
unit VARCHAR(20) NOT NULL DEFAULT 'cm'
created_by UUID NOT NULL
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NULL
updated_by UUID NULL
deleted_at TIMESTAMP NULL
deleted_by UUID NULL
```

Do not add `property_id` in the MVP unless a real use case appears.

The box model primarily describes a product/item, not a property. Property context can be inferred later through purchases/reservations/maintenance usage if needed.

### `product_box_model_faces`

This is an image relationship table and must follow the hard-delete image policy.

Recommended columns:

```text
id UUID PK DEFAULT gen_random_uuid()
organization_id UUID NOT NULL
product_box_model_id UUID NOT NULL
face_name VARCHAR(20) NOT NULL
s3_key VARCHAR(500) NOT NULL
filepath VARCHAR(300) NOT NULL
original_filename VARCHAR(255) NOT NULL
content_type VARCHAR(100) NOT NULL
size_bytes BIGINT NOT NULL
rotation_degrees NUMERIC(10,2) NULL
flip_horizontal BOOLEAN NOT NULL DEFAULT false
flip_vertical BOOLEAN NOT NULL DEFAULT false
created_by UUID NOT NULL
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NULL
updated_by UUID NULL
```

Do **not** add `deleted_at` or `deleted_by` to `product_box_model_faces`.

Valid `face_name` values:

```text
front
back
left
right
top
bottom
```

Add a unique constraint:

```text
UNIQUE(product_box_model_id, face_name)
```

## S3 key strategy

Use the existing organization-first strategy.

Recommended key:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Recommended filepath:

```text
{bucket}/{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}
```

Reason:

- Keeps files grouped by organization.
- Keeps product box models grouped with catalog/product-related assets.
- Keeps each face easy to inspect manually.
- Avoids mixing product box images with normal inventory item images.

## Delete policy

### Face delete

When a face image is deleted:

1. Delete the S3 object first.
2. If S3 deletion succeeds, physically delete the `product_box_model_faces` row.
3. If S3 deletion fails, do not delete the database row.

### Face replace

When replacing a face image:

1. Validate the model belongs to the current organization.
2. Upload the new image to S3.
3. Delete the previous face image from S3 if one exists.
4. Update/replace the face metadata row.
5. If the previous S3 delete fails, abort the metadata replacement and clean up the newly uploaded object when possible.

### Model delete

When deleting a product box model:

1. Load all face rows for the model.
2. Delete each S3 object.
3. If all storage deletes succeed, physically delete face rows.
4. Soft-delete the parent `product_box_models` row.

The parent model may use soft delete because it is a business entity. Face rows must use hard delete because they represent S3-backed images.

## Backend package recommendation

Create a new module package instead of putting this under `image` or `catalog`:

```text
backend/src/main/java/com/tamias/productbox/
```

Suggested structure:

```text
productbox/controller
productbox/dto
productbox/entity
productbox/enums
productbox/mapper
productbox/repository
productbox/service
```

Reason:

- Product Box Models have their own lifecycle.
- The module combines metadata, images and later frontend 3D rendering support.
- Keeping it separate avoids bloating existing inventory/purchase/image packages.

## Backend API plan

### 14A endpoints — metadata only

```text
GET /api/v1/product-box-models
GET /api/v1/product-box-models/{id}
POST /api/v1/product-box-models
PUT /api/v1/product-box-models/{id}
DELETE /api/v1/product-box-models/{id}
```

Optional metadata lookup endpoints:

```text
GET /api/v1/inventory-items/{id}/box-models
GET /api/v1/purchase-items/{id}/box-models
```

Use plural `box-models` because an item may eventually have more than one model, for example original box, replacement packaging or different packaging versions.

### 14B endpoints — face images

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}
PUT /api/v1/product-box-models/{id}/faces/{faceName}
DELETE /api/v1/product-box-models/{id}/faces/{faceName}
```

The `GET` model endpoints should include faces and presigned URLs after 14B.

## Frontend plan

Create a new feature folder:

```text
frontend/src/app/features/product-box-models/
```

Suggested files:

```text
components/product-box-viewer/
components/product-box-face-upload/
pages/product-box-models-page/
pages/product-box-model-form-page/
models/product-box-model.model.ts
services/product-box-model.service.ts
```

Translation namespaces:

```text
productBoxModels
productBoxModels.faces
productBoxModels.viewer
```

Translation files:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Do not create feature-specific translation services.

## Three.js plan

Use Three.js only in the frontend. The viewer should:

- Accept dimensions and unit.
- Accept a `faces` object keyed by `front`, `back`, `left`, `right`, `top`, `bottom`.
- Build `THREE.BoxGeometry(width, height, depth)`.
- Apply materials in the correct Three.js BoxGeometry order:

```text
right
left
top
bottom
front
back
```

- Use placeholder materials for missing faces.
- Support orbit/rotation and zoom.
- Dispose renderer, geometry, materials and textures in `ngOnDestroy`.
- Preprocess textures in 14D.1 so transparent or border-background padding is cropped and fitted to the face ratio.

## Incremental implementation plan

### 14A — Product Box Models backend foundation

Metadata CRUD only.

- Flyway migration for `product_box_models`.
- Entity, DTOs, repository, mapper, service and controller.
- Organization scoping.
- Optional association with `inventory_items` and `purchase_items`.
- No S3, no faces, no frontend.

### 14B — Product Box Face Images

- Flyway migration for `product_box_model_faces`.
- Upload/replace/delete one face image.
- Hard delete face images from S3.
- Presigned URLs in model detail responses.
- No Three.js yet.

### 14C — Angular Product Box CRUD

- List page.
- Form page/modal.
- Static i18n labels in `en.json` and `es.json`.
- No Three.js yet except perhaps a placeholder panel.

### 14D — Three.js Product Box Viewer

- Reusable viewer component.
- Render box with face textures.
- Orbit controls and cleanup.

### 14D.1 — Auto texture crop and fit

- Frontend-only canvas preprocessing.
- Crop transparent or border-background padding.
- Fit each processed texture to the target face aspect ratio.
- Do not modify original S3 images.

### 14E — Inventory/Purchase integration

- Show models from inventory item context.
- Show models from purchase item context if the UI has a suitable detail/modal.
- Add indicators where useful.

### 14F — AI awareness

- Read-only AI tools for product box metadata.
- TAMI can answer whether an item has a model, what dimensions it has and which faces are available.
- No image interpretation.

## Acceptance criteria for Phase 14 overall

- Product box models are scoped by organization.
- Product box metadata is stored in PostgreSQL.
- Face images are stored in private S3, not PostgreSQL byte arrays.
- Face image deletion leaves no S3 garbage after successful operations.
- Angular renders a simple rectangular box dynamically using metadata + image URLs.
- The viewer crops/fits texture padding without modifying original uploaded images.
- The module does not break existing inventory, purchase, image, S3, AI or RAG behavior.

## Implementation status

- 14A completed: backend metadata CRUD and `product_box_models`.
- 14B completed: `product_box_model_faces`, S3 upload/replace/delete, hard-delete face rows and presigned URLs in model responses.
- 14C completed: Angular Product Box CRUD and face upload UI.
- 14D completed: Three.js Product Box Viewer.
- 14D.1 completed: Auto texture crop and fit in the viewer.
- 14E next: Integration with Inventory/Purchases.
