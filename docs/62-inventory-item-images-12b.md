# 12B — Inventory Item Images

## Purpose

Allow images to be attached to inventory items.

This helps identify supplies, materials, amenities, cleaning products, tools and other operational items visually.

---

## Dependency

This phase depends on:

- 11A — S3 key strategy + filepath fields
- 11B — Hard delete policy for entity images
- 12A — Brand association with inventory items

---

## Database

Create:

```text
inventory_item_images
```

Fields mirror the current `property_images` pattern as much as possible, but with `inventory_item_id` as parent FK.

Required logical fields:

```text
id
organization_id
inventory_item_id
original_filename
s3_key
filepath
content_type
size_bytes
is_cover
status
created_at
created_by
```

Rules:

- Do not add `deleted_at` or `deleted_by`.
- Do not soft-delete rows.
- Delete must physically remove the S3/storage object first and then physically delete the DB row.
- If deleting the S3/storage object fails, do not delete the DB row.
- Parent inventory item must belong to the current organization.

---

## S3 path

Use the final 11A storage strategy with `organizationId` as the first level inside the bucket:

```text
{organizationId}/catalogs/inventory_items/{inventoryItemId}/{filename}
```

Example:

```text
s3_key: 7a3a8c8e-1111-4444-9999-111122223333/catalogs/inventory_items/289d3236-2f55-4759-83c3-01d3407228e2/Image1.jpg
filepath: tamias-dev-files/7a3a8c8e-1111-4444-9999-111122223333/catalogs/inventory_items/289d3236-2f55-4759-83c3-01d3407228e2
```

Bucket must come from configuration.

---

## Backend API

Endpoint pattern:

```text
GET    /api/v1/inventory-items/{inventoryItemId}/images
GET    /api/v1/inventory-items/{inventoryItemId}/images/{imageId}
POST   /api/v1/inventory-items/{inventoryItemId}/images
PATCH  /api/v1/inventory-items/{inventoryItemId}/images/{imageId}/cover
GET    /api/v1/inventory-items/{inventoryItemId}/images/{imageId}/file
DELETE /api/v1/inventory-items/{inventoryItemId}/images/{imageId}
```

Rules:

- Parent inventory item must belong to current organization.
- Upload must use configured file size/content type validations.
- Delete must delete S3/storage object and DB row physically.
- The storage key must include organization ID.

---

## Frontend

Inventory Items catalog includes an images action.

UI pattern:

```text
Inventory Items table
  Images button
    opens modal
      list existing images
      upload one or more images
      optionally mark image as principal
      delete image
```

Inventory item names in selectors/searchers should continue to show:

```text
{item} - {brand}
```

The Inventory Items table should continue to show item name and brand in separate columns.

---

## AI impact

Future AI image metadata tools should be able to answer questions such as:

```text
¿Qué items tienen imágenes?
¿Qué imágenes tengo para el item cloro?
```

Not required in this phase unless existing file/image tools are updated at the same time.

---

## Acceptance tests

1. Open Inventory Items catalog.
2. Click Images for an item.
3. Upload one image.
4. Confirm image appears in modal.
5. Upload multiple images.
6. Confirm all images appear in modal.
7. Confirm S3 key uses `{organizationId}/catalogs/inventory_items/{inventoryItemId}/`.
8. Confirm `filepath` is populated as `{bucket}/{organizationId}/catalogs/inventory_items/{inventoryItemId}`.
9. Delete image.
10. Confirm S3 object is gone.
11. Confirm DB row is gone.
12. Confirm user from another organization cannot access/delete image.
