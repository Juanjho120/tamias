# 12C — Purchase List Images

Status: Completed.

## Purpose

Allow multiple images to be attached to a purchase list.

Examples:

```text
receipts
invoice screenshots
product photos
proof of delivery
store photos
```

## User decision

The FK must be:

```text
purchase_list_id
```

Do not use `purchase_id` unless the actual domain entity is renamed in the future.

## Dependency

This phase depends on:

```text
11A — S3 key strategy + filepath fields
11B — Hard delete policy for entity images
```

## Database

Created table:

```text
purchase_images
```

Required logical fields:

```text
id
organization_id
purchase_list_id
s3_key
filepath
filename/original_filename
content_type
size_bytes
status
created_at
created_by
```

Rules:

- Do not add soft-delete columns.
- Use hard delete only.
- Before implementing or changing this module, inspect current purchase list table/entity names.

## S3 path

Use:

```text
{organizationId}/purchases/{purchaseListId}/{filename}
```

Example:

```text
s3_key: 7a3a8c8e-1111-4444-9999-111122223333/purchases/8d58a26f-11a1-4a80-a851-eab88de142f2/receipt.jpg
filepath: tamias-dev-files/7a3a8c8e-1111-4444-9999-111122223333/purchases/8d58a26f-11a1-4a80-a851-eab88de142f2
```

Bucket must come from configuration. The final S3 strategy keeps `organizationId` as the first level inside the bucket.

## Backend API

Endpoint pattern:

```http
GET    /api/v1/purchase-lists/{purchaseListId}/images
GET    /api/v1/purchase-lists/{purchaseListId}/images/{imageId}
POST   /api/v1/purchase-lists/{purchaseListId}/images
GET    /api/v1/purchase-lists/{purchaseListId}/images/{imageId}/file
DELETE /api/v1/purchase-lists/{purchaseListId}/images/{imageId}
```

Rules:

- Parent purchase list must belong to current organization.
- Delete must remove S3 object and DB row physically.
- If S3/storage deletion fails, the DB row must not be deleted.
- Do not allow image access across organizations.

## Frontend

Purchase Lists screen includes an images action.

UI pattern:

```text
Purchase Lists table
Images button
opens modal
list existing images
upload one or more images
delete image through app ConfirmModalComponent
```

The implemented modal must follow the same UX pattern used by properties and maintenance images:

- Selected image preview before upload.
- Clear button to reset selected files.
- Upload button aligned with Clear on the right side of the modal.
- Upload button uses the Bootstrap upload icon: `bi bi-upload`.
- Delete confirmation uses `ConfirmModalComponent`, not native browser confirmation dialogs.
- File input must restrict the picker to JPG, PNG and WEBP:

```html
accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
```

All visible labels and messages must use the current static i18n strategy:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Translations for this modal live under:

```text
purchases.images
```

Do not hardcode Spanish or English UI text in components/templates.

## AI impact

AI image metadata tools were implemented later in 12E. The relevant tool is:

```text
images.getPurchaseListImages
```

It supports questions such as:

```text
¿Qué listas de compra tienen imágenes?
¿Qué compras no tienen fotos?
¿Qué imágenes tiene la compra de La Torre?
```

## Acceptance tests

1. Open Purchase Lists screen.
2. Click Images for a purchase list.
3. Upload several images.
4. Confirm images appear in modal.
5. Confirm selected files show previews before upload.
6. Confirm Clear resets selected files and previews.
7. Confirm the file picker filters JPG, PNG and WEBP.
8. Confirm `s3_key` starts with `{organizationId}/purchases/{purchaseListId}/`.
9. Confirm `filepath` is populated as `{bucket}/{organizationId}/purchases/{purchaseListId}`.
10. Delete one image.
11. Confirm delete uses `ConfirmModalComponent`, not native browser dialogs.
12. Confirm S3 object is gone.
13. Confirm DB row is gone.
14. Confirm another organization cannot access/delete image.
