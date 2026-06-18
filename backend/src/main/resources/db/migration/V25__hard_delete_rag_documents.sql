-- 11C — RAG documents use hard delete.
-- Future document deletions physically remove Chroma vectors, S3 object, document_chunks, and documents rows.
-- This migration removes legacy soft-deleted document metadata rows from PostgreSQL and prevents new DELETED statuses.
-- The deleted_at/deleted_by columns remain for compatibility with existing read-only metadata queries, but the delete flow no longer writes them.

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

ALTER TABLE documents DROP CONSTRAINT IF EXISTS chk_documents_status;
ALTER TABLE documents ADD CONSTRAINT chk_documents_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
