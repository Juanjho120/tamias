# 14G — OpenCV Perspective Correction Backend

Status: **Implemented**

## Purpose

Add the backend processing step that converts an original phone photo of a Product Box face into a clean rectangular processed texture using OpenCV Java.

This phase processes manual four-corner points only. It does not include the Angular corner editor, accept/retry workflow, automatic contour detection or image enhancement.

## Context

Previous Product Box phases implemented:

- `14A` Product Box Models backend foundation.
- `14B` Product Box Face Images.
- `14C` Angular Product Box CRUD.
- `14D` Three.js Product Box Viewer.
- `14E` Product Box 3D Textures architecture/design.
- `14F` Texture metadata + original upload.

14F added original and processed metadata columns to `product_box_model_faces`. 14G uses those fields to generate and store the processed preview texture.

## Dependency

OpenCV Java is provided by:

```text
org.openpnp:opencv:4.9.0-0
```

Reason:

- It packages OpenCV Java bindings with native libraries.
- It avoids manually installing OpenCV in every developer or deployment environment during the MVP.

Risk:

- The dependency is large, approximately 109 MB.
- Render/Docker deployment size should be watched after this phase.

## Endpoint

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

Coordinates are real image pixels from the original uploaded image.

## Processing flow

1. Validate the Product Box Model and face belong to the current organization.
2. Validate that the face has an `original_s3_key`.
3. Load the original image from S3/storage.
4. Decode image bytes into an OpenCV `Mat`.
5. Validate the four points are inside image bounds.
6. Validate the polygon area is meaningful.
7. Calculate target aspect ratio from Product Box dimensions:
   - `front/back = width / height`
   - `left/right = depth / height`
   - `top/bottom = width / depth`
8. Generate a target canvas where the longest side is currently `1600px`.
9. Use OpenCV `Imgproc.getPerspectiveTransform`.
10. Use OpenCV `Imgproc.warpPerspective`.
11. Encode the result as PNG.
12. Store the processed texture in S3.
13. Delete the previous processed texture from S3 if it existed.
14. Update `product_box_model_faces` metadata.

## S3 path

Processed texture path:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/processed/{filename}
```

## Updated face metadata

14G updates:

```text
processed_s3_key
processed_filepath
processed_filename
processed_content_type
processed_size_bytes
processed_width_px
processed_height_px
target_aspect_ratio
points_json
texture_status = PROCESSED
processing_error
processed_at
updated_by
```

The accepted/active texture is **not** changed in 14G.

```text
s3_key = accepted/active texture used by Three.js
processed_s3_key = latest processed preview texture
```

14I will accept a processed texture and promote it to `s3_key`.

## Error behavior

If processing fails:

- `texture_status` is set to `FAILED` when possible.
- `processing_error` stores a truncated error message.
- The active accepted texture is not modified.
- The original uploaded image is not deleted.

If S3 deletion of the previous processed texture fails after a new processed file was uploaded:

1. The new processed file is deleted as best effort.
2. The operation fails.
3. DB metadata is not advanced to the new processed texture.

## Not included

- No Angular corner editor.
- No automatic contour detection.
- No lighting/contrast enhancement.
- No accept/retry/delete texture workflow.
- No AI image interpretation.
- No generated `.glb`/`.gltf`.

## Acceptance criteria

- Backend can process four manually selected corner points.
- Processed output respects the real selected face aspect ratio.
- Processed output is stored in S3 under the organization-first path.
- Face response includes processed metadata and processed presigned URL.
- The active accepted texture remains unchanged until 14I.
- Reprocessing deletes the superseded processed image from S3.
- Processing failures do not accidentally accept a texture.

## Next phase

Proceed with:

```text
14H — Angular corner editor + processed texture preview
```
