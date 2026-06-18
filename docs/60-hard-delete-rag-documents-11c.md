# 11C — Hard Delete Policy for RAG Documents

## Purpose

Change RAG document deletion so it physically removes all related data from:

```text
Chroma vector entries
S3 original document object
PostgreSQL document_chunks
PostgreSQL documents
```

Documents must not use soft delete after this phase.

---

## Applies to

```text
documents
document_chunks
Chroma vector entries
S3 original document object
```

---

## Implemented delete flow

When a document is deleted:

```text
1. Validate authenticated user.
2. Validate organization ownership.
3. Load document metadata.
4. Delete vectors from Chroma using document_chunks.vector_store_id.
5. Delete original file from configured storage/S3 using documents.s3_key.
6. Delete document_chunks rows.
7. Delete documents row.
```

The chosen order is intentional:

```text
Chroma first
S3 second
PostgreSQL last
```

This keeps PostgreSQL rows intact if Chroma or S3 fails. PostgreSQL is deleted last because DB changes can roll back, but external Chroma/S3 side effects cannot.

---

## Failure behavior

### Chroma delete fails

```text
Abort deletion.
Do not delete S3 file.
Do not delete PostgreSQL rows.
Return controlled error from the backend.
```

### S3 delete fails

```text
Abort deletion.
Do not delete PostgreSQL rows.
Return controlled error from the backend.
```

If Chroma succeeds but S3 fails, PostgreSQL rows remain and the document can be re-indexed if needed. This avoids deleting the uploaded file while still having a DB document row.

### DB delete fails

```text
Return controlled error.
Log document id, organization id and s3_key.
```

PostgreSQL transaction rollback cannot restore external S3/Chroma side effects, so DB deletion stays last.

---

## Schema cleanup

Legacy soft-deleted document rows are physically removed by migration:

```text
V25__hard_delete_rag_documents.sql
```

The migration:

```text
1. Deletes document_chunks for documents already marked DELETED or deleted_at IS NOT NULL.
2. Deletes those documents rows.
3. Updates the documents status check constraint to allow only ACTIVE and INACTIVE.
```

The delete flow no longer writes:

```text
documents.deleted_at
documents.deleted_by
documents.status = DELETED
```

The `deleted_at` and `deleted_by` columns are kept for compatibility with existing read-only metadata queries. They can be removed later in a dedicated schema cleanup after all metadata/tool queries are migrated away from `d.deleted_at IS NULL`.

---

## Storage path note

Document uploads use the organization-scoped S3 strategy:

```text
{organizationId}/documents/{filename}
{organizationId}/documents/{propertyId}/{filename}
```

The persisted `filepath` keeps the bucket + folder without the filename:

```text
{bucket}/{organizationId}/documents
{bucket}/{organizationId}/documents/{propertyId}
```

---

## Acceptance tests

```text
1. Upload document without property.
2. Confirm s3_key starts with {organizationId}/documents/.
3. Upload document with property.
4. Confirm s3_key starts with {organizationId}/documents/{propertyId}/.
5. Process document.
6. Index document.
7. Confirm /api/v1/ai/search returns sources from the document.
8. Delete document.
9. Confirm S3 object is gone.
10. Confirm documents row is gone.
11. Confirm document_chunks rows are gone.
12. Confirm Chroma no longer returns the document.
13. Confirm AI metadata tools no longer list the document.
14. Simulate S3 delete failure and confirm DB rows remain.
15. Simulate Chroma delete failure and confirm DB rows remain.
```

---

## Out of scope

- RAG retrieval tuning. That belongs to 9P-I if needed.
- Reprocessing/reindex retry UX.
- Document versioning.
- Removing legacy `documents.deleted_at` / `documents.deleted_by` columns from the schema.
