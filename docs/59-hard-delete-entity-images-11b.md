# 11B — Hard Delete Policy for Entity Images

## Purpose

Entity image deletion must remove both:

```text
1. the object from storage
2. the database relationship row
```

Image relationship tables must not use soft delete. This avoids new garbage files in S3 and stale image rows in PostgreSQL.

---

## Implemented in this phase

The existing image tables are:

```text
property_images
maintenance_record_images
```

The future tables remain out of scope until their own phases:

```text
inventory_item_images   -> 12B
purchase_images         -> 12C
reservation_images      -> 12D
```

---

## Schema changes

Migration:

```text
V24__hard_delete_entity_images.sql
```

The migration removes the soft-delete fields from existing image tables:

```text
property_images.deleted_at
property_images.deleted_by
maintenance_record_images.deleted_at
maintenance_record_images.deleted_by
```

It also removes existing rows already marked as soft-deleted before dropping the columns:

```sql
DELETE FROM property_images
WHERE status = 'DELETED'
   OR deleted_at IS NOT NULL;

DELETE FROM maintenance_record_images
WHERE status = 'DELETED'
   OR deleted_at IS NOT NULL;
```

Important note: if older soft-deleted rows already left orphaned files in S3, those S3 objects cannot be safely removed from a PostgreSQL migration. This phase prevents new garbage from being created going forward.

---

## Runtime delete behavior

Expected flow:

```text
1. Validate authenticated user.
2. Validate organization ownership of the parent entity.
3. Find the active image row scoped by parent entity + organization.
4. Delete the object from storage using s3_key.
5. Physically delete the database row.
```

If storage deletion fails:

```text
- abort operation
- keep database row
- return controlled error
```

Because the service methods are transactional, the database delete is not committed unless the storage deletion succeeds.

---

## Updated storage contract

`FileStorageService` now exposes:

```java
void delete(String storageKey);
```

Implemented by:

```text
S3FileStorageService
LocalFileStorageService
```

For S3, deletion uses the configured bucket and the stored `s3_key`.

---

## Multi-tenant rule

Do not delete an image only by image id. Always scope through the parent entity and organization.

```text
property image -> image id + property id + organization id + ACTIVE status
maintenance image -> image id + maintenance record id + organization id + ACTIVE status
```

---

## Query rule after this phase

Do not query image tables with:

```sql
deleted_at IS NULL
```

The columns no longer exist. Use active metadata instead:

```sql
status = 'ACTIVE'
```

---

## Acceptance tests

```text
1. Upload property image.
2. Delete property image.
3. Confirm S3 object no longer exists.
4. Confirm property_images row no longer exists.
5. Confirm no deleted_at/deleted_by columns exist for property_images.
6. Repeat for maintenance images.
7. Attempt deleting an image from another organization.
8. Operation must be denied/not found.
9. Confirm AI file/image metadata tools still work without deleted_at queries.
```

---

## Out of scope

```text
- RAG document deletion. That belongs to 11C.
- S3 path strategy. That belongs to 11A.
- Adding new image modules. Those belong to 12B, 12C and 12D.
```
