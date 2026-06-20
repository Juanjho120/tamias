# 14 — Product Box Models

Status: **In progress / completed through 14L**

## Purpose

Add a Product Box Models module to TAMIAS so users can reconstruct simple rectangular product/package boxes using metadata, S3-backed face images and a Three.js viewer.

The module supports boxes for products such as cleaning supplies, electronics, appliances, food packaging, spare parts and accessories used in property operations.

## Current architecture

Product Box Models are implemented as a dedicated backend/frontend module:

```text
backend/src/main/java/com/tamias/productbox
frontend/src/app/features/product-box-models
```

Key decisions:

- PostgreSQL stores metadata and S3 keys only.
- S3 stores images privately and exposes them through presigned URLs.
- Product Box parent models use soft delete.
- Face images/textures use hard delete.
- The frontend rebuilds the box dynamically with Three.js.
- The backend does not render 3D and does not generate `.glb`/`.gltf` files in the MVP.
- Product Box 3D Texture processing uses OpenCV Java for faithful geometry/perspective correction.
- Optional AI texture enhancement may be added after OpenCV processing, but it must not replace the OpenCV baseline.

## Implemented subphases

```text
14A — Product Box Models backend foundation Completed
14B — Product Box Face Images Completed
14C — Angular Product Box CRUD Completed
14D — Three.js Product Box Viewer Completed
14E — Product Box 3D Textures architecture/design Completed
14F — Texture metadata + original upload Completed
14G — OpenCV perspective correction backend Completed
14H — Angular corner editor + processed texture preview Completed
14I — Accept/retry/delete texture workflow Completed
14J — Automatic contour detection and image enhancement Completed
14K — AI Texture Enhancement architecture/design Completed
14L — AI Texture metadata and backend provider abstraction Completed
```

## Pending subphases

```text
14M — AI Texture enhancement backend                    Completed Planned next
14N — Angular AI enhanced preview and accept workflow  Next Planned
14O — Integration with Inventory/Purchases Planned
14P — AI awareness for Product Box Models Planned
```

## Data model

Main table:

```text
product_box_models
```

Face/texture table:

```text
product_box_model_faces
```

`product_box_model_faces.s3_key` remains the active/accepted texture used by the Three.js viewer.

Additional texture lifecycle fields support:

- original upload,
- processed preview,
- accepted texture,
- four-corner points,
- OpenCV processing metadata,
- automatic contour detection metadata,
- enhancement mode,
- future optional AI-enhanced texture metadata.

## Face names

```text
front
back
left
right
top
bottom
```

Three.js `BoxGeometry` material order:

```text
right
left
top
bottom
front
back
```

## Texture lifecycle

```text
UPLOADED        original image exists, no processed texture yet
POINTS_SELECTED contour/manual points have been selected
PROCESSED       processed OpenCV preview exists
ACCEPTED        processed/direct image is active in the viewer
FAILED          last processing attempt failed
```

Future AI enhancement must add its own draft/enhancement status without changing the meaning of the existing OpenCV lifecycle.

## S3 structure

Organization id remains the first path segment.

Original uploads:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/original/{filename}
```

Processed previews:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/processed/{filename}
```

Accepted/direct textures:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Future AI-enhanced drafts:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/enhanced/{filename}
```

## Hard delete policy

Face texture delete must delete S3 objects before deleting the DB row:

- `s3_key`,
- `original_s3_key`,
- `processed_s3_key`,
- future `ai_enhanced_s3_key`.

If any required S3 deletion fails, the database row must not be deleted.

## OpenCV texture processing

The OpenCV pipeline supports:

- manual four-corner perspective correction,
- automatic contour detection as a helper,
- real face aspect-ratio output,
- image enhancement,
- processed texture preview,
- explicit accept/save workflow.

Automatic detection never replaces manual review. It only pre-fills the four points in the Angular editor.

## Optional AI texture enhancement

AI enhancement is planned as an optional visual enhancement step after OpenCV processing.

OpenCV remains the faithful baseline. AI output must be stored separately and accepted explicitly by the user before becoming the active texture.

AI metadata is now present on `product_box_model_faces` through 14L. The AI-enhanced draft key is stored separately in `ai_enhanced_s3_key`, and `active_texture_source` records whether the active `s3_key` came from a direct upload, OpenCV or a future AI-enhanced output.

The active texture still remains `product_box_model_faces.s3_key`.

## Next phase

```text
14M — AI Texture enhancement backend                    Completed
```
