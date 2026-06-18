# 11A — S3 Key Strategy + Filepath Fields

## Purpose

Replace the old year/month-based upload structure with module/entity-based folders in S3.

This keeps uploaded files close to the operational entity they belong to and makes cleanup/auditing easier.

## Implemented folders in this phase

This phase updates only the entities/modules that already exist before phases 12B–12D:

```text
tamias-dev-files/
  properties/
    {propertyId}/
      Image1.jpg
  maintenance/
    {maintenanceRecordId}/
      Image1.jpg
  documents/
    Document1.pdf
    {propertyId}/
      Document1.pdf
```

Documents follow this rule:

```text
Document without property -> documents/{filename}
Document with property    -> documents/{propertyId}/{filename}
```

The following folders are reserved for later phases:

```text
reservations/{reservationId}/                 -> 12D
catalogs/inventory_items/{inventoryItemId}/   -> 12B
purchases/{purchaseListId}/                   -> 12C
```

## filepath definition

Added `filepath VARCHAR(300)` to:

```text
documents
property_images
maintenance_record_images
```

Future image tables must also include the same column:

```text
reservation_images
purchase_images
inventory_item_images
```

Definition:

```text
filepath = bucket + folder path, without filename
```

Example:

```text
bucket: tamias-dev-files
s3_key: properties/078dab91-b46d-4378-9672-ad2c16cf50cf/Image1.jpg
filepath: tamias-dev-files/properties/078dab91-b46d-4378-9672-ad2c16cf50cf
filename: Image1.jpg
```

`filepath` is for traceability/reporting. `s3_key` remains the value used to access/delete the object in S3.

## Migration notes

The migration adds the new columns as nullable so existing rows with old year/month keys keep working.

New uploads must populate `filepath`.

Existing rows may remain null unless a manual backfill is intentionally performed later.

## Acceptance tests

```text
1. Upload a property image.
2. Confirm s3_key starts with properties/{propertyId}/.
3. Confirm filepath is {bucket}/properties/{propertyId}.
4. Upload a maintenance image.
5. Confirm s3_key starts with maintenance/{maintenanceRecordId}/.
6. Confirm filepath is {bucket}/maintenance/{maintenanceRecordId}.
7. Upload a document without property.
8. Confirm s3_key starts with documents/ and has no property folder.
9. Confirm filepath is {bucket}/documents.
10. Upload a document with property.
11. Confirm s3_key starts with documents/{propertyId}/.
12. Confirm filepath is {bucket}/documents/{propertyId}.
13. Download URLs still work.
14. Existing old files do not crash list/download flows.
```

## Out of scope

- Hard delete behavior. That belongs to 11B and 11C.
- Inventory item images. That belongs to 12B.
- Purchase list images. That belongs to 12C.
- Reservation images. That belongs to 12D.
