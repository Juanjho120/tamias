# 14I — Accept/Retry/Delete Texture Workflow

Status: **Implemented**

## Purpose

Complete the Product Box 3D texture MVP workflow after 14F–14H:

```text
upload original → adjust 4 points → process OpenCV texture → preview → accept → use in Three.js
```

This phase promotes a processed preview texture to the active face texture used by the Three.js viewer, keeps retry behavior safe, and keeps hard-delete cleanup consistent with TAMIAS image policies.

## Scope

Included:

- Backend endpoint to accept a processed texture.
- Backend endpoint to delete the complete face texture lifecycle.
- Frontend buttons to accept a processed texture and retry corner adjustment.
- Hide the corner editor automatically after successful processing.
- Preserve accepted texture while preparing a new original/processed draft.
- Avoid deleting the accepted texture when `processed_s3_key` and `s3_key` point to the same object.
- Keep hard-delete behavior for S3-backed face images.

Not included:

- Automatic contour detection.
- Automatic lighting/contrast correction.
- Inventory/Purchases integration.
- AI awareness.
- Irregular packages or `.glb` export.

## Backend endpoints

### Accept processed texture

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/accept
```

Rules:

- Requires an existing `processed_s3_key`.
- Deletes the previous active `s3_key` from S3 if it is different from `processed_s3_key`.
- Promotes processed metadata to active metadata:
  - `s3_key = processed_s3_key`
  - `filepath = processed_filepath`
  - `original_filename = processed_filename`
  - `content_type = processed_content_type`
  - `size_bytes = processed_size_bytes`
- Sets:
  - `texture_status = ACCEPTED`
  - `accepted_at = now`
  - `processing_error = null`

The accepted texture is the one used by the Three.js viewer.

### Delete texture

```text
DELETE /api/v1/product-box-models/{id}/faces/{faceName}/texture
```

This endpoint deletes the full face texture lifecycle:

- active accepted texture: `s3_key`,
- original upload: `original_s3_key`,
- processed preview: `processed_s3_key`,
- physical row from `product_box_model_faces`.

Deletion keeps the hard-delete rule:

1. Delete S3 objects first.
2. If storage deletion succeeds, delete the DB row.
3. If storage deletion fails, do not delete the DB row.

## Retry workflow

Retry does not need a dedicated backend endpoint in the MVP.

The user can retry by:

1. Reopening the corner editor for the original photo.
2. Moving points.
3. Running OpenCV processing again.

When processing again:

- a new processed texture is stored in S3,
- the previous draft processed texture is removed when safe,
- if the previous processed key is already the accepted active key, it is not deleted,
- the accepted texture remains visible in the Three.js viewer until the user accepts the new processed preview.

When uploading a new original photo:

- old original and draft processed files are deleted,
- the accepted texture remains active if it exists,
- status moves back to `UPLOADED` for the new draft workflow.

## Frontend behavior

The Product Box faces modal now shows:

- active texture,
- original photo,
- processed texture preview.

For processed texture preview, it provides:

- `Accept texture`,
- `Retry adjustment`,
- open preview link.

After successful processing, the corner editor is closed automatically so the user sees the processed preview immediately.

## i18n

Translations are provided as addition files:

```text
frontend/public/assets/i18n/es.14i-product-box-texture-workflow.additions.json
frontend/public/assets/i18n/en.14i-product-box-texture-workflow.additions.json
```

These blocks must be merged into:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

No new translation service is introduced.

## Acceptance criteria

- User can process a texture and the corner editor closes after success.
- User can see the processed preview.
- User can accept the processed preview.
- Accepted preview becomes the active `s3_key` used by Three.js.
- User can retry processing without deleting the currently accepted texture.
- User can delete a face texture and S3/DB cleanup is hard-delete and consistent.
- No native browser confirm dialogs are introduced.
- Existing direct active face upload from 14B remains available.

## Next phase

`14J — Automatic contour detection and image enhancement`

That phase will use OpenCV to attempt automatic rectangular contour detection and conservative image enhancement, while keeping manual point editing as the fallback.
