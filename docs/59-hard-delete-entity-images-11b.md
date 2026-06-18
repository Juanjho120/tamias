# 11B — Hard Delete Policy for Entity Images

## Purpose

Ensure entity image deletion removes both:

```text
1. the S3 object
2. the database relationship row
```

Image relationship tables must not use soft delete. This avoids garbage files in S3 and stale image records in PostgreSQL.

---

## Applies to

```text
property_images
maintenance_record_images
reservation_images
purchase_images
inventory_item_images
```

Some of these tables already exist and some will be added in later phases.

---

## Schema rule

Image relationship tables must not have soft-delete fields.

If any existing image table has:

```text
deleted_at
deleted_by
status = DELETED
```

clean the schema according to the actual current table design.

The specific user decision for this phase is:

```text
Clean schema and remove deleted_at from image relationship tables where present.
```

Before creating migrations, inspect the current Flyway migrations and entity classes. Do not assume columns exist.

---

## Delete behavior

Expected flow:

```text
1. Validate authenticated user.
2. Validate organization ownership of parent entity.
3. Find image row.
4. Delete file from S3 using s3_key.
5. Delete database row physically.
```

If S3 delete fails:

```text
abort operation
keep database row
return controlled error
```

If database delete fails after S3 delete:

```text
return controlled error
log enough context for manual investigation
```

For MVP, prefer strict behavior to avoid silent inconsistencies.

---

## Multi-tenant rule

Do not delete an image only by image id.

Always scope through the parent entity and organization.

Example pattern:

```text
property image -> image id + property id + organization id
maintenance image -> image id + maintenance record id + organization id
reservation image -> image id + reservation id + organization id
purchase image -> image id + purchase list id + organization id
inventory item image -> image id + inventory item id + organization id
```

Use actual entity/repository names from the codebase.

---

## filepath behavior

All image rows must store:

```text
s3_key
filepath
original_filename or filename, according to existing pattern
content_type
size_bytes
```

`filepath` comes from 11A and must not include filename.

---

## Acceptance tests

```text
1. Upload property image.
2. Delete property image.
3. Confirm S3 object no longer exists.
4. Confirm property_images row no longer exists.
5. Confirm no deleted_at/status soft delete was applied.
6. Repeat for maintenance images.
7. After 12B/12C/12D, repeat for inventory item, purchase list and reservation images.
8. Attempt deleting an image from another organization.
9. Operation must be denied/not found.
```

---

## Out of scope

- RAG document deletion. That belongs to 11C.
- S3 path strategy. That belongs to 11A.
- Adding new image modules. Those belong to 12B, 12C and 12D.
