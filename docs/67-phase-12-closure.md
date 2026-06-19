# 12 — Images, inventory brands and AI tool closure

Status: Completed.

## Purpose

This document closes the Phase 12 workstream and records the final implementation decisions after phases 12A, 12B, 12C, 12D and 12E.

Phase 12 expanded TAMIAS in three connected areas:

1. Inventory item brand ownership.
2. Image support for inventory items, purchase lists and reservations.
3. AI tools for image metadata, item brands and inventory usage.

## Completed phases

### 12A — Associate brands directly with inventory items

Completed behavior:

- `brand_id` belongs to `inventory_items`.
- `purchase_items` derives brand from its related inventory item.
- Inventory item selectors/searchers display `{item name} - {brand}` when brand exists.
- Inventory item catalog tables continue to display item name and brand in separate columns.
- Maintenance details, reservations supplies and purchase list items display item + brand where selection/search context needs disambiguation.

### 12B — Inventory item images

Completed behavior:

- Added `inventory_item_images`.
- Parent FK: `inventory_item_id`.
- Uses hard delete.
- Deletes storage/S3 object before deleting DB row.
- Uses S3 key strategy:

```text
{organizationId}/catalogs/inventory_items/{inventoryItemId}/{filename}
```

- Uses filepath strategy:

```text
{bucket}/{organizationId}/catalogs/inventory_items/{inventoryItemId}
```

- Frontend has an images modal in the Inventory Items catalog.
- Modal follows the same UX pattern as properties and maintenance images.

### 12C — Purchase list images

Completed behavior:

- Added `purchase_images`.
- Parent FK: `purchase_list_id`.
- Uses hard delete.
- Deletes storage/S3 object before deleting DB row.
- Uses S3 key strategy:

```text
{organizationId}/purchases/{purchaseListId}/{filename}
```

- Uses filepath strategy:

```text
{bucket}/{organizationId}/purchases/{purchaseListId}
```

- Frontend has an images modal in Purchase Lists.
- Modal follows the same UX pattern as properties and maintenance images.

### 12D — Reservation images

Completed behavior:

- Added `reservation_images`.
- Parent FK: `reservation_id`.
- Uses hard delete.
- Deletes storage/S3 object before deleting DB row.
- Uses S3 key strategy:

```text
{organizationId}/reservations/{reservationId}/{filename}
```

- Uses filepath strategy:

```text
{bucket}/{organizationId}/reservations/{reservationId}
```

- Frontend has an images modal in Reservations.
- Modal follows the same UX pattern as properties and maintenance images.

### 12E — AI image and inventory brand tools

Completed behavior:

- Added image metadata tools for reservations, inventory items and purchase lists.
- Updated inventory tools to include brand information.
- Added items-by-brand tool.
- Improved routing so questions using `productos` can route to inventory when the user means inventory items, not purchase analytics.
- Improved extraction to avoid using control words like `no` or `he` as search terms.

## Final image table policy

Applies to:

```text
property_images
maintenance_record_images
inventory_item_images
purchase_images
reservation_images
```

Rules:

- Entity image tables do not use soft delete.
- New image tables must not include `deleted_at` or `deleted_by`.
- Deleting an image must first delete the storage/S3 object.
- If storage/S3 delete fails, the DB row must not be deleted.
- After successful storage delete, physically delete the DB row.
- All reads/writes must filter by `organization_id`.
- Image records should not expose cross-organization data.

## Final S3 key policy

All entity files/images use organization as the first folder inside the bucket:

```text
{organizationId}/{module}/{entityId}/{filename}
```

Current target structure:

```text
{organizationId}/properties/{propertyId}/
{organizationId}/maintenance/{maintenanceRecordId}/
{organizationId}/catalogs/inventory_items/{inventoryItemId}/
{organizationId}/purchases/{purchaseListId}/
{organizationId}/reservations/{reservationId}/
{organizationId}/documents/
{organizationId}/documents/{propertyId}/
```

`filepath` stores:

```text
{bucket}/{organizationId}/{module}/{entityId}
```

without the filename.

## Final image modal UX policy

All image upload modals should follow the same UX baseline:

- Existing image list.
- Multi-file upload.
- Selected file preview before upload.
- Clear button for selected files.
- Clear and Upload buttons aligned to the right.
- Upload button icon: `bi bi-upload`.
- Delete confirmation through app modal/component, not native browser confirm.
- File picker restricted to JPG, PNG and WEBP:

```html
accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"
```

## Final i18n policy for image modals

Do not create feature-specific translation services or runtime translation registration files.

Use the existing frontend i18n strategy:

```text
frontend/public/assets/i18n/es.json
frontend/public/assets/i18n/en.json
```

Module-specific translation locations:

```text
properties.images
maintenance.images
catalogs.items.inventoryItems.images
purchases.images
reservations.images
```

Do not hardcode Spanish or English text in modal templates/components.

## AI tool evidence expectations

For image and inventory questions, TAMI should prefer system tools over RAG when the question is about structured TAMIAS data.

Examples:

```text
¿Qué reservaciones tienen imágenes?      -> images.getReservationImages
¿Qué items no tienen imágenes?           -> images.getInventoryItemImages
¿Qué compras no tienen fotos?            -> images.getPurchaseListImages
¿Qué items tengo de la marca Pledge?     -> inventory.getItemsByBrand
¿Qué productos tengo por marca?          -> inventory.getItemsByBrand
¿Qué productos tengo en inventario?      -> inventory.search
¿Cuáles son los productos más usados?    -> inventory.getFrequentlyUsed
¿Dónde he usado covertor elástico?       -> inventory.whereUsed + module-specific usage tools
```

RAG should not be used as the primary answer source for these structured questions unless the system tools cannot answer and the question clearly asks about uploaded documents.

## Regression checklist

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
npm run build
```

Manual flows:

1. Upload/delete property image.
2. Upload/delete maintenance image.
3. Upload/delete inventory item image.
4. Upload/delete purchase list image.
5. Upload/delete reservation image.
6. Confirm no orphan DB row remains after successful delete.
7. Confirm no orphan S3 object remains after successful delete.
8. Confirm file pickers only show JPG, PNG and WEBP by default.
9. Confirm delete confirmation uses app modal, not native browser confirm.
10. Ask TAMI the 12E validation questions and verify tool evidence.

## Next phase

Next recommended phase:

```text
13A — AI image/file dashboard tools
```

After 13A is completed, resume the existing AI orchestration roadmap:

```text
9P-G — AI orchestration observability and debug traces
9P-H — Smoke test hardening / final fixes
9P-I — RAG retrieval tuning, only if needed
```
