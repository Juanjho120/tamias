# 65. Remove document soft-delete leftovers — 11C.1

## Goal

Finish the hard-delete policy introduced in 11C for RAG documents by removing the remaining soft-delete model from `documents`.

After this phase, documents are not soft-deleted. When a document is deleted, TAMIAS must remove:

1. Chroma vector entries associated with the document chunks.
2. The uploaded file from storage/S3.
3. `document_chunks` rows associated with the document.
4. The `documents` row.

## Scope

### Backend schema

Add migration:

```text
backend/src/main/resources/db/migration/V26__remove_document_soft_delete_columns.sql
```

The migration:

- Deletes legacy rows where `documents.status = 'DELETED'` or `documents.deleted_at IS NOT NULL`.
- Deletes chunks belonging to those legacy documents.
- Drops `documents.deleted_at`.
- Drops `documents.deleted_by`.

### Backend entity/repository

- Remove `deletedAt` and `deletedBy` from `Document`.
- Remove `DELETED` from `DocumentStatus`.
- Replace `DocumentRepository` methods ending in `DeletedAtIsNull` with organization/status/type/property-scoped methods only.
- Update `DocumentService` to use the new repository methods.

### AI metadata queries

Remove stale SQL predicates that referenced document soft-delete columns:

```sql
AND d.deleted_at IS NULL
```

Because documents are physically deleted, active metadata queries no longer need that predicate.

### Image query cleanup

Entity image tables already moved to hard delete in 11B. Any remaining AI metadata predicates against removed image soft-delete columns must also be removed:

```sql
AND pi.deleted_at IS NULL
AND mri.deleted_at IS NULL
```

Image visibility should now rely on row existence, organization scoping, entity scoping, and `status = 'ACTIVE'` where that column is still part of the image table.

## Out of scope

- Changing Chroma deletion behavior. That was already handled in 11C through `RagVectorStoreService.deleteDocumentVectors(document)`.
- Adding new image tables for inventory items, purchases, or reservations.
- Changing S3 key strategy. That was handled in 11A.

## Validation checklist

1. Run Flyway migrations successfully.
2. Confirm `documents.deleted_at` no longer exists.
3. Confirm `documents.deleted_by` no longer exists.
4. Upload a document.
5. Process and index the document.
6. Confirm RAG can retrieve content from it.
7. Delete the document.
8. Confirm the storage object is removed.
9. Confirm `document_chunks` rows are removed.
10. Confirm the `documents` row is removed.
11. Confirm RAG no longer returns that document.
12. Run AI document metadata prompts:
    - `¿Qué documentos tengo cargados?`
    - `¿Qué documentos tengo por tipo?`
    - `¿Cómo está el índice RAG de mis documentos?`
    - `¿Qué archivos asociados a documentos tengo?`

## Decision

`documents` now follows the same hard-delete principle as RAG storage and Chroma vectors. Keeping soft-delete columns after 11C would make the model ambiguous and could reintroduce stale metadata checks in AI tools.
