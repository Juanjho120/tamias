-- 11C.1: Remove document soft-delete leftovers.
-- RAG documents now use hard delete only: Chroma vectors, storage object, chunks, and document row.

DELETE FROM document_chunks
WHERE document_id IN (
    SELECT id
    FROM documents
    WHERE status = 'DELETED'
       OR deleted_at IS NOT NULL
);

DELETE FROM documents
WHERE status = 'DELETED'
   OR deleted_at IS NOT NULL;

ALTER TABLE documents
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE documents
    DROP COLUMN IF EXISTS deleted_by;
