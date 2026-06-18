# 11C — Hard Delete Policy for RAG Documents

## Purpose

Change RAG document deletion so it physically removes all related data from:

```text
S3
Chroma
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

## Required delete flow

When a document is deleted:

```text
1. Validate authenticated user.
2. Validate organization ownership.
3. Load document metadata.
4. Delete original file from S3 if it exists.
5. Delete vectors from Chroma for that document.
6. Delete document_chunks rows.
7. Delete documents row.
```

The user decision for this phase is:

```text
Abort the transaction if Chroma or S3 cleanup fails.
```

---

## Failure behavior

### S3 delete fails

```text
Abort deletion.
Do not delete DB rows.
Do not delete Chroma vectors.
Return controlled error.
```

### Chroma delete fails

```text
Abort deletion.
Do not delete DB rows.
Return controlled error.
```

If S3 was already deleted before Chroma fails, log enough detail to repair manually. To reduce this risk, implementation may delete Chroma first and then S3, but the final order must be intentionally chosen after reviewing current services.

### DB delete fails

```text
Return controlled error.
Log document id, organization id and s3_key.
```

---

## Transaction boundary note

PostgreSQL transaction cannot automatically roll back S3 or Chroma side effects.

Implementation must be explicit and careful.

Recommended approach:

```text
1. Validate everything first.
2. Confirm document chunks/vector ids exist.
3. Execute external cleanup with controlled exceptions.
4. Delete DB rows last inside transaction.
```

If the current code already has a different processing/indexing service flow, review it line by line before changing.

---

## Chroma cleanup requirement

Delete vectors by stable document metadata, preferably:

```text
document_id
organization_id
```

Do not rely only on text matching.

If vector ids are stored in `document_chunks.vector_store_id`, use those ids when supported by the current Chroma integration.

---

## Schema cleanup

If `documents` currently has soft-delete columns such as:

```text
deleted_at
deleted_by
status = DELETED
```

create a migration to remove or stop using them according to the actual current schema.

Do not remove columns blindly without checking existing queries, DTOs, tools and frontend filters.

---

## Acceptance tests

```text
1. Upload document.
2. Process document.
3. Index document.
4. Confirm /api/v1/ai/search returns sources from the document.
5. Delete document.
6. Confirm S3 object is gone.
7. Confirm documents row is gone.
8. Confirm document_chunks rows are gone.
9. Confirm Chroma no longer returns the document.
10. Confirm AI metadata tools no longer list the document.
11. Simulate S3 delete failure and confirm DB rows remain.
12. Simulate Chroma delete failure and confirm DB rows remain.
```

---

## Out of scope

- RAG retrieval tuning. That belongs to 9P-I if needed.
- Reprocessing/reindex retry UX.
- Document versioning.
