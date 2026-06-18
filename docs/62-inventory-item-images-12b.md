# 12B — Inventory Item Images

## Purpose

Allow images to be attached to inventory items.

This helps identify supplies, materials, amenities, cleaning products, tools and other operational items visually.

---

## Dependency

This phase depends on:

```text
11A — S3 key strategy + filepath fields
11B — Hard delete policy for entity images
12A — Brand association with inventory items
```

---

## Database

Create:

```text
inventory_item_images
```

Fields should match the current `property_images` pattern as much as possible, but with:

```text
inventory_item_id
filepath
```

Required logical fields:

```text
id
inventory_item_id
s3_key
filepath
filename/original_filename
content_type
size_bytes
created_at
created_by
```

Do not add soft-delete columns.

Before implementing, inspect the actual current `property_images` and `maintenance_record_images` schemas/entities and mirror the real naming conventions.

---

## S3 path

Use:

```text
{organizationId}/catalogs/inventory_items/{inventoryItemId}/{filename}
```

Example:

```text
s3_key: catalogs/inventory_items/289d3236-2f55-4759-83c3-01d3407228e2/Image1.jpg
filepath: tamias-dev-files/catalogs/inventory_items/289d3236-2f55-4759-83c3-01d3407228e2
```

Bucket must come from configuration.

---

## Backend API

Recommended endpoint pattern:

```http
GET    /api/v1/inventory-items/{inventoryItemId}/images
POST   /api/v1/inventory-items/{inventoryItemId}/images
DELETE /api/v1/inventory-items/{inventoryItemId}/images/{imageId}
```

Rules:

- Parent inventory item must belong to current organization.
- Upload must use configured file size/content type validations.
- Delete must delete S3 object and DB row physically.

---

## Frontend

Inventory Items catalog should include an images action.

UI pattern:

```text
Inventory Items table
  Images button
    opens modal
      list existing images
      upload one or more images
      delete image
```

Use the same UX pattern already used by property images and maintenance images.

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

```text
1. Open Inventory Items catalog.
2. Click Images for an item.
3. Upload image.
4. Confirm image appears in modal.
5. Confirm S3 key uses catalogs/inventory_items/{inventoryItemId}/.
6. Confirm filepath is populated.
7. Delete image.
8. Confirm S3 object is gone.
9. Confirm DB row is gone.
10. Confirm user from another organization cannot access/delete image.
```
