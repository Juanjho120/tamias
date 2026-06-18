# 11A — S3 Key Strategy + Filepath Fields

## Purpose

Replace the current year/month S3 folder structure with a module/entity-based structure that is easier to understand, clean up and audit.

Current year/month structure is not useful for TAMIAS because uploaded files are operationally tied to entities such as properties, maintenance records, reservations, purchase lists, inventory items and documents.

---

## Target S3 structure

```text
tamias-dev-files/
  properties/
    {propertyId}/
      Image1.jpg
      Image2.jpg
  reservations/
    {reservationId}/
      Image1.jpg
  catalogs/
    inventory_items/
      {inventoryItemId}/
        Image1.jpg
  maintenance/
    {maintenanceRecordId}/
      Image1.jpg
  purchases/
    {purchaseListId}/
      Image1.jpg
  documents/
    Document1.pdf
    Document2.pdf
    {propertyId}/
      Document1.pdf
```

Documents rule:

```text
Document without property -> documents/{filename}
Document with property    -> documents/{propertyId}/{filename}
```

---

## filepath definition

Add `filepath VARCHAR(300)` to:

```text
documents
property_images
maintenance_record_images
reservation_images
purchase_images
inventory_item_images
```

Definition:

```text
filepath = bucket + full folder path, without filename
```

Example:

```text
bucket: tamias-dev-files
s3_key: properties/078dab91-b46d-4378-9672-ad2c16cf50cf/Image1.jpg
filepath: tamias-dev-files/properties/078dab91-b46d-4378-9672-ad2c16cf50cf
filename: Image1.jpg
```

Important:

```text
filepath is for traceability/reporting.
s3_key is still the value used to access/delete the object in S3.
```

---

## Bucket source

The bucket name must come from the current environment/configuration.

Development example:

```text
tamias-dev-files
```

Production must use the production bucket from environment/configuration.

Do not hardcode `tamias-dev-files` in business logic.

---

## Backend design recommendation

Introduce a central S3 key builder to avoid path duplication.

Possible concept:

```text
S3FilePathService
S3KeyBuilder
FileStoragePathResolver
```

Responsibilities:

```text
buildPropertyImageKey(propertyId, filename)
buildMaintenanceImageKey(maintenanceRecordId, filename)
buildReservationImageKey(reservationId, filename)
buildPurchaseImageKey(purchaseListId, filename)
buildInventoryItemImageKey(inventoryItemId, filename)
buildDocumentKey(propertyId nullable, filename)
buildFilepath(s3Key)
```

Before implementing, check existing storage service/class names and reuse the current architecture.

---

## Migration notes

This phase should add new columns and update new upload behavior.

Recommended approach:

```text
1. Add filepath nullable first if existing rows exist.
2. New uploads must populate filepath.
3. Existing rows may remain null unless a backfill is intentionally implemented.
4. Do not break existing files that still use old s3_key paths.
```

If a backfill is implemented, derive `filepath` from existing `s3_key` and configured bucket.

---

## Acceptance tests

```text
1. Upload a property image.
2. Confirm s3_key starts with properties/{propertyId}/.
3. Confirm filepath is bucket + properties/{propertyId}.
4. Upload a maintenance image.
5. Confirm s3_key starts with maintenance/{maintenanceRecordId}/.
6. Upload a document without property.
7. Confirm s3_key starts with documents/ and no property folder.
8. Upload a document with property.
9. Confirm s3_key starts with documents/{propertyId}/.
10. Download URLs still work.
11. Existing old files do not crash list/download flows.
```

---

## Out of scope

- Hard delete behavior. That belongs to 11B and 11C.
- Inventory item images. That belongs to 12B.
- Purchase list images. That belongs to 12C.
- Reservation images. That belongs to 12D.
