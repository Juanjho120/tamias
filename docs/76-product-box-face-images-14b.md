# 14B — Product Box Face Images

Status: **Completed**

## Purpose

Add image support for Product Box Models by allowing one private S3-backed image per box face. This phase extends the 14A backend foundation without adding Angular UI or Three.js rendering yet.

## Scope

Included:

- Flyway migration for `product_box_model_faces`.
- JPA entity, repository, service and DTO for box faces.
- Upload or replace an image for each valid face.
- Delete a face image using hard delete.
- Include face metadata and presigned URLs in Product Box Model responses.
- Delete all face S3 objects and hard-delete face rows before soft-deleting a parent model.
- Use the existing `ImageValidationService` for JPG, PNG and WEBP validation.
- Use the existing private storage and presigned URL flow through `FileStorageService`.
- Keep all queries organization-scoped.

Not included:

- No Angular CRUD.
- No Three.js viewer.
- No `.glb` or generated 3D assets.
- No AI tools.
- No OCR or vision model analysis.

## Migration

14A introduced `V32__create_product_box_models.sql`, so 14B adds:

```text
V33__create_product_box_model_faces.sql
```

Table:

```text
product_box_model_faces
```

Important columns:

```text
organization_id
product_box_model_id
face_name
s3_key
filepath
original_filename
content_type
size_bytes
rotation_degrees
flip_horizontal
flip_vertical
created_by
updated_by
created_at
updated_at
```

Do not add `deleted_at` or `deleted_by` to this table. Face rows represent S3-backed entity images and must be physically deleted when the image is deleted.

Valid faces:

```text
front
back
left
right
top
bottom
```

Unique rule:

```text
UNIQUE(product_box_model_id, face_name)
```

## S3 strategy

Use the organization-first S3 key strategy already used by TAMIAS:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/{filename}
```

Expected filepath:

```text
{bucket}/{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}
```

## API

14B keeps the existing model endpoints from 14A and adds face endpoints:

```text
GET    /api/v1/product-box-models/{id}/faces
GET    /api/v1/product-box-models/{id}/faces/{faceName}
POST   /api/v1/product-box-models/{id}/faces/{faceName}
PUT    /api/v1/product-box-models/{id}/faces/{faceName}
GET    /api/v1/product-box-models/{id}/faces/{faceName}/file
DELETE /api/v1/product-box-models/{id}/faces/{faceName}
```

`POST` and `PUT` consume `multipart/form-data` with:

```text
file
rotationDegrees optional
flipHorizontal optional
flipVertical optional
```

`POST` and `PUT` both behave as upload-or-replace for MVP simplicity.

## Response shape

`ProductBoxModelResponse` now includes a `faces` object keyed by face name:

```json
{
  "id": "...",
  "name": "Caja cafetera Oster",
  "width": 30,
  "height": 40,
  "depth": 20,
  "unit": "cm",
  "faces": {
    "front": {
      "id": "...",
      "faceName": "front",
      "imageKey": "...",
      "filepath": "...",
      "originalFilename": "front.jpg",
      "contentType": "image/jpeg",
      "sizeBytes": 12345,
      "rotationDegrees": null,
      "flipHorizontal": false,
      "flipVertical": false,
      "imageUrl": "presigned-url",
      "imageUrlExpiresIn": 900
    }
  }
}
```

## Delete policy

### Face delete

1. Validate the model belongs to the current organization.
2. Delete the S3 object using `fileStorageService.delete(s3Key)`.
3. Only if storage deletion succeeds, hard-delete the `product_box_model_faces` row.

### Face replace

1. Validate the model belongs to the current organization.
2. Upload the new image to S3.
3. If a previous face exists, delete the old S3 object.
4. If old S3 deletion fails, attempt best-effort cleanup of the newly uploaded object and abort the replacement.
5. Update the existing face row or create a new row.

### Model delete

When deleting a Product Box Model:

1. Load all face rows for the model.
2. Delete each S3 object first.
3. If storage deletion succeeds for all faces, hard-delete the face rows.
4. Soft-delete the parent `product_box_models` row.

## Security and tenancy

All face operations must validate:

```text
product_box_models.organization_id = currentUser.organization_id
product_box_models.deleted_at IS NULL
product_box_model_faces.organization_id = currentUser.organization_id
```

The frontend never sends `organizationId`.

## Manual tests

1. Create a Product Box Model from 14A.
2. Upload `front`, `back`, `left`, `right`, `top` and `bottom` images.
3. Verify only JPG, PNG and WEBP are accepted.
4. Verify the model detail response includes `faces` with presigned URLs.
5. Replace `front` and confirm the old S3 object is removed.
6. Delete one face and confirm the S3 object and DB row are removed.
7. Delete the model and confirm all remaining face S3 objects are removed before the parent model is soft-deleted.
8. Confirm another organization cannot access, replace or delete the faces.

## Commands

```bash
cd backend
./mvnw test
```
