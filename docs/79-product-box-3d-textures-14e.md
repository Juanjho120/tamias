# 14E — Product Box 3D Textures architecture/design

Status: **Completed / design ready**

## Purpose

Design the next Product Box Models increment: a non-generative image-processing workflow that turns phone photos of each product-box face into clean rectangular textures for the Three.js viewer.

This phase is documentation/design only. Implementation starts in 14F.

## Current TAMIAS context

TAMIAS already has:

- `product_box_models` as the parent business entity.
- `product_box_model_faces` as the canonical face/texture table.
- Private S3 face image storage with presigned URLs.
- Hard delete for product box face image rows and S3 objects.
- Angular Product Box CRUD.
- Three.js Product Box Viewer using accepted face image URLs.
- Organization-scoped backend access using the authenticated user's organization.

Therefore this new workflow must extend the existing Product Box module instead of creating an unrelated module.

## Key decision

Do **not** create a separate `product_box_textures` module as the canonical texture owner.

Use the existing table:

```text
product_box_model_faces
```

as the canonical per-face texture record, and extend it with original/processed/accepted texture metadata.

The existing `s3_key` remains the active/accepted texture used by the Three.js viewer. New fields will store the original upload and the latest processed preview.

## Non-generative image processing

This feature must not use generative AI.

The pipeline uses OpenCV Java for faithful image processing:

- manual four-corner selection,
- perspective correction,
- crop,
- rotation/enderezado through homography,
- resize to the real face aspect ratio,
- basic image enhancement,
- preview,
- accept/save.

Automatic contour detection is allowed as an assistive feature, but the manual four-corner editor remains the fallback and source of truth when detection is imperfect.

## Face aspect ratio rules

The backend computes target aspect ratio from the model dimensions and selected face.

```text
front/back  = width / height
left/right  = depth / height
top/bottom  = width / depth
```

For a box:

```text
width  = 27.5 cm
height = 65 cm
depth  = 23 cm
```

The target ratios are:

```text
front/back  = 27.5:65
left/right  = 23:65
top/bottom  = 27.5:23
```

The processed texture must not be stretched arbitrarily. It must be generated to match the selected face proportion.

## Product box texture lifecycle

Recommended statuses:

```text
UPLOADED
POINTS_SELECTED
PROCESSED
ACCEPTED
FAILED
```

Meaning:

- `UPLOADED`: original photo exists in S3, but no processed texture is ready.
- `POINTS_SELECTED`: corner points have been saved or sent for processing.
- `PROCESSED`: processed texture exists and can be previewed.
- `ACCEPTED`: processed texture is the active texture used by the viewer.
- `FAILED`: the last processing attempt failed; error metadata is available.

## Data model evolution

Extend `product_box_model_faces` in 14F/14G instead of replacing it.

Existing active texture fields stay:

```text
s3_key
filepath
original_filename
content_type
size_bytes
rotation_degrees
flip_horizontal
flip_vertical
```

New fields recommended:

```text
original_s3_key VARCHAR(500) NULL
original_filepath VARCHAR(300) NULL
original_filename_raw VARCHAR(255) NULL
original_content_type VARCHAR(100) NULL
original_size_bytes BIGINT NULL
original_width_px INTEGER NULL
original_height_px INTEGER NULL

processed_s3_key VARCHAR(500) NULL
processed_filepath VARCHAR(300) NULL
processed_filename VARCHAR(255) NULL
processed_content_type VARCHAR(100) NULL
processed_size_bytes BIGINT NULL
processed_width_px INTEGER NULL
processed_height_px INTEGER NULL

target_aspect_ratio NUMERIC(12,6) NULL
points_json JSONB NULL
texture_status VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED'
processing_error TEXT NULL
processed_at TIMESTAMP NULL
accepted_at TIMESTAMP NULL

auto_detected_points BOOLEAN NOT NULL DEFAULT false
contour_confidence NUMERIC(5,4) NULL
enhancement_mode VARCHAR(20) NOT NULL DEFAULT 'basic'
```

The exact migration may be split across subphases, but the model should preserve these concepts.

## Points JSON

The point order must be explicit:

```json
{
  "topLeft": { "x": 123, "y": 456 },
  "topRight": { "x": 789, "y": 450 },
  "bottomRight": { "x": 800, "y": 1200 },
  "bottomLeft": { "x": 110, "y": 1210 }
}
```

Coordinates should be sent in real image pixels after the frontend maps displayed-image coordinates back to the original uploaded image size.

If normalized coordinates are introduced later, the request DTO must clearly identify them and the backend must convert them consistently.

## S3 structure

Keep organization id as the first path segment.

Original uploads:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/original/{filename}
```

Processed previews:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/processed/{filename}
```

Accepted/active texture:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

`filepath` fields follow the existing TAMIAS convention: configured bucket plus folder path without filename.

## Hard delete policy

Face images remain hard-delete assets.

When deleting a face texture, delete all related S3 objects first, then physically delete the face row:

- active/accepted texture `s3_key`,
- original image `original_s3_key`,
- processed preview `processed_s3_key`.

If any required S3 delete fails, do not delete the database row.

When replacing original or processed files, delete superseded S3 objects to avoid storage garbage.

When deleting a parent Product Box Model, delete all associated face S3 objects and face rows before soft-deleting the parent model.

## Endpoint design

Use the existing route prefix style:

```text
/api/v1/product-box-models/{id}/faces/{faceName}/...
```

Recommended endpoints:

```text
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/original
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/process
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/accept
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/detect-contour
DELETE /api/v1/product-box-models/{id}/faces/{faceName}/texture
```

Existing endpoints for basic face upload/delete may remain for simple upload flows, but the new texture workflow should become the preferred path for clean 3D textures.

## OpenCV processing pipeline

Backend service name recommendation:

```text
ProductBoxTextureProcessingService
```

Processing responsibilities:

1. Load original image from S3/storage.
2. Decode image bytes into OpenCV `Mat`.
3. Validate image type and dimensions.
4. Validate four points.
5. Order points as top-left, top-right, bottom-right, bottom-left if needed.
6. Calculate target dimensions from face aspect ratio.
7. Build source and destination point matrices.
8. Use `Imgproc.getPerspectiveTransform`.
9. Use `Imgproc.warpPerspective`.
10. Apply basic enhancement if enabled.
11. Encode result as PNG or JPEG.
12. Store processed texture in S3.
13. Persist metadata and status.

MVP should prefer manual points first. Automatic detection can reuse the same process endpoint once points are accepted or adjusted by the user.

## Automatic contour detection

Automatic detection is planned after the manual workflow is complete.

Recommended endpoint:

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/detect-contour
```

Success response shape:

```json
{
  "detected": true,
  "confidence": 0.86,
  "points": {
    "topLeft": { "x": 123, "y": 456 },
    "topRight": { "x": 789, "y": 450 },
    "bottomRight": { "x": 800, "y": 1200 },
    "bottomLeft": { "x": 110, "y": 1210 }
  },
  "message": "Contour detected successfully"
}
```

Failure response shape:

```json
{
  "detected": false,
  "confidence": 0.31,
  "points": null,
  "message": "No reliable rectangular contour was detected. Please adjust the corners manually."
}
```

The frontend should always allow manual adjustment even when detection succeeds.

## Basic image enhancement

Enhancement should be conservative and faithful to the original photo.

Recommended modes:

```text
none
basic
strong
```

MVP default:

```text
basic
```

Candidate operations:

- brightness/contrast normalization,
- light shadow reduction,
- optional CLAHE if results are acceptable,
- optional white-balance helper later.

Do not over-process colors. Product packaging should remain visually faithful.

## Frontend design

The existing Product Box Models Angular feature should gain a texture editor flow.

Editor requirements:

- Show original photo.
- Overlay four draggable points.
- Connect points with lines.
- Keep point coordinates mapped to the original image pixels.
- Button: `Process texture`.
- Preview processed texture returned by backend.
- Button: `Apply to model` or `Accept and save`.
- Retry flow.
- Delete texture flow using existing confirm modal pattern.
- Static translations in `frontend/public/assets/i18n/es.json` and `en.json`.

The Three.js viewer keeps using the accepted face image URLs. It should not need OpenCV logic.

## Dependency decision

Backend currently does not use OpenCV.

Preferred implementation candidate:

```text
org.openpnp:opencv
```

Reason:

- Provides Java bindings and native loading support.
- Avoids manually installing system OpenCV on every environment during MVP.

Risk:

- The dependency is large.
- Deployment image/build size may increase.
- Render deployment should be validated during 14G before continuing too far.

14G must document any Docker/Render impact if the OpenCV runtime requires it.

## Incremental subphases

Use this new sequence for Product Box Models after 14D:

```text
14E — Product Box 3D Textures architecture/design
14F — Texture metadata + original upload
14G — OpenCV perspective correction backend
14H — Angular corner editor + processed texture preview
14I — Accept/retry/delete texture workflow
14J — Automatic contour detection and image enhancement
14K — Integration with Inventory/Purchases
14L — AI awareness for Product Box Models
```

## Acceptance criteria for the full texture MVP

1. User can select a Product Box Model face.
2. User can upload an original phone photo.
3. User can mark four corners.
4. Backend processes the image with OpenCV perspective correction.
5. Processed result respects the real face aspect ratio.
6. User can preview processed texture.
7. User can accept/save the processed texture.
8. Three.js viewer applies accepted texture to the correct face.
9. Original and processed images are stored in private S3.
10. Metadata is stored in PostgreSQL.
11. Reprocessing/replacing a face deletes superseded S3 objects.
12. Deleting a face deletes original/processed/accepted S3 objects and physically deletes the row.
13. Processing failures are persisted and shown clearly.
14. Automatic contour detection can prefill points, but manual adjustment remains available.
15. Basic enhancement improves usability without changing the product identity.

## Out of scope for 14E

- No code.
- No Flyway migration yet.
- No OpenCV dependency yet.
- No frontend editor yet.
- No AI image interpretation.
- No generated `.glb`/`.gltf`.
- No destructive image edits outside S3 replacement/delete flows.

## Next implementation phase

Proceed with:

```text
14F — Texture metadata + original upload
```

14F should add the database fields and backend upload endpoint for original photos without implementing perspective correction yet.
