# 14F — Product Box Texture Metadata + Original Upload

Status: **Implemented**

## Purpose

Extend Product Box face images so TAMIAS can store the original photo that will later be processed into a clean 3D texture.

This phase does **not** run OpenCV yet. It prepares the database, entity model, response DTOs and upload endpoint needed by the later processing phases.

## Context

Previous Product Box phases implemented:

- `14A` Product Box Models backend foundation.
- `14B` Product Box Face Images.
- `14C` Angular Product Box CRUD.
- `14D` Three.js Product Box Viewer.
- `14E` Product Box 3D Textures architecture/design.

Before 14F, `product_box_model_faces.s3_key` represented the active image used by the Three.js viewer.

14F keeps that rule:

```text
s3_key = accepted/active texture used by the viewer
```

and adds separate metadata for the original uploaded photo:

```text
original_s3_key = source photo uploaded by the user for future OpenCV processing
```

## Scope

Included:

- Add texture metadata columns to `product_box_model_faces`.
- Allow face rows without an accepted `s3_key` yet.
- Add texture status tracking.
- Add original upload endpoint.
- Store original image in private S3.
- Return original presigned URL in face response.
- Keep delete behavior hard-delete for all S3-backed face files.

Not included:

- No OpenCV perspective correction yet.
- No corner editor UI yet.
- No processed texture generation yet.
- No accept/retry workflow yet.
- No automatic contour detection yet.
- No lighting enhancement yet.

## Migration

Migration:

```text
V34__add_product_box_texture_metadata.sql
```

Adds:

```text
original_s3_key
original_filepath
original_upload_filename
original_content_type
original_size_bytes
original_width_px
original_height_px
processed_s3_key
processed_filepath
processed_filename
processed_content_type
processed_size_bytes
processed_width_px
processed_height_px
target_aspect_ratio
points_json
texture_status
processing_error
processed_at
accepted_at
```

Also relaxes active image columns so a face can exist with an original upload but no accepted texture yet:

```text
s3_key nullable
filepath nullable
original_filename nullable
content_type nullable
size_bytes nullable
```

## Texture statuses

```text
UPLOADED
POINTS_SELECTED
PROCESSED
ACCEPTED
FAILED
```

14F only creates/uses:

```text
UPLOADED
ACCEPTED
```

Later phases will use the remaining statuses.

## S3 paths

Original upload key folder:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/original/{filename}
```

Active/accepted texture folder remains:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Future processed texture folder:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/processed/{filename}
```

## Endpoint

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/original
```

Consumes:

```text
multipart/form-data
file: JPG, PNG or WEBP
```

Response: `ProductBoxModelFaceResponse` with original metadata and `originalImageUrl`.

Additional read endpoint:

```text
GET /api/v1/product-box-models/{id}/faces/{faceName}/texture/original/file
```

## Delete behavior

When deleting a face:

1. Delete active accepted image from S3 if it exists.
2. Delete original image from S3 if it exists.
3. Delete processed image from S3 if it exists.
4. Physically delete the `product_box_model_faces` row.

When deleting a Product Box Model:

1. Load all face rows.
2. Delete active/original/processed S3 objects for every face.
3. Physically delete all face rows.
4. Soft-delete the parent Product Box Model.

If S3 deletion fails, DB deletion must not continue.

## Acceptance criteria

- User can upload an original photo for any valid face.
- Original image is stored in S3 under the organization-first path.
- Face response includes original metadata and presigned URL.
- Existing accepted textures continue working with the Three.js viewer.
- A face can exist with an original upload but no accepted texture yet.
- Deleting a face/model removes original/processed/active S3 objects when present.
