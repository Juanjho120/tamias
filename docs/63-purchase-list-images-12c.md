# 12C — Purchase List Images

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

---

## User decision

The FK must be:

```text
purchase_list_id
```

Do not use `purchase_id` unless the actual domain entity is renamed in the future.

---

## Dependency

This phase depends on:

```text
11A — S3 key strategy + filepath fields
11B — Hard delete policy for entity images
```

---

## Database

Create:

```text
purchase_images
```

Required logical fields:

```text
id
purchase_list_id
s3_key
filepath
filename/original_filename
content_type
size_bytes
created_at
created_by
```

Do not add soft-delete columns.

Before implementing, inspect current purchase list table/entity names.

---

## S3 path

Use:

```text
{organizationId}/purchases/{purchaseListId}/{filename}
```

Example:

```text
s3_key: purchases/8d58a26f-11a1-4a80-a851-eab88de142f2/receipt.jpg
filepath: tamias-dev-files/purchases/8d58a26f-11a1-4a80-a851-eab88de142f2
```

Bucket must come from configuration.

---

## Backend API

Recommended endpoint pattern:

```http
GET    /api/v1/purchase-lists/{purchaseListId}/images
POST   /api/v1/purchase-lists/{purchaseListId}/images
DELETE /api/v1/purchase-lists/{purchaseListId}/images/{imageId}
```

Rules:

- Parent purchase list must belong to current organization.
- Delete must remove S3 object and DB row physically.
- Do not allow image access across organizations.

---

## Frontend

Purchase Lists screen should include an images action.

UI pattern:

```text
Purchase Lists table
  Images button
    opens modal
      list existing images
      upload one or more images
      delete image
```

Use the same pattern as properties and maintenance images.

---

## Acceptance tests

```text
1. Open Purchase Lists screen.
2. Click Images for a purchase list.
3. Upload several images.
4. Confirm images appear in modal.
5. Confirm s3_key starts with purchases/{purchaseListId}/.
6. Confirm filepath is populated.
7. Delete one image.
8. Confirm S3 object is gone.
9. Confirm DB row is gone.
10. Confirm another organization cannot access/delete image.
```
