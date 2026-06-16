# 9P-F — RAG Diagnostics Playbook

## Purpose

Use this playbook when TAMIAS AI does not find information from PDFs/documents that appear to be uploaded, processed, and indexed.

The goal is to isolate whether the failure is in:

- File storage/S3 metadata
- Document processing
- Chunk creation
- Vector indexing
- Chroma/vector store collection
- Query routing/planning
- Similarity threshold/topK
- Property/organization filtering
- Answer generation

## Important observation from 9P-F

A previous RAG failure was resolved by deleting the old documents, uploading them again, processing them, and indexing them again. The likely cause was old document/file metadata still pointing to local backend upload paths instead of the S3-backed storage configuration.

This is a valid remediation path when the environment changed from local uploads to S3 and old files were uploaded before the storage provider switch.

## Fast diagnostic checklist

### 1. Confirm the document record exists

Use the document list endpoint or UI and verify:

```text
document exists
status = ACTIVE
processingStatus = PROCESSED
s3Key is present
contentType is correct
sizeBytes > 0
propertyId is correct or intentionally null
```

If the file was uploaded before switching to S3, prefer re-uploading it instead of trying to patch metadata manually.

### 2. Confirm the file is retrievable

Use the download URL/file endpoint.

Expected:

```text
The file can be downloaded/opened.
The content is the expected PDF/document.
No local path dependency remains.
```

If download fails, fix storage/S3 before debugging RAG.

### 3. Confirm processing generated chunks

Use:

```http
GET /api/v1/documents/{documentId}/chunks
```

Expected:

```text
At least one chunk exists.
Chunks contain readable text.
Chunks are not empty.
The target phrase exists in at least one chunk if you expect RAG to find it.
```

If chunks are empty, the problem is document text extraction, not vector search.

### 4. Confirm chunks were indexed

For every relevant chunk, verify:

```text
vectorStoreCollection is not null
vectorStoreId is not null
```

If chunks exist but vector IDs are null, run indexing again.

### 5. Confirm AI metadata tools agree

Ask:

```text
¿Cómo está el índice RAG de mis documentos?
¿Qué documentos están listos para IA?
¿Qué documentos están procesados pero no indexados para IA?
¿Qué documentos fallaron al procesarse?
```

Expected:

```text
The document should appear as processed and indexed.
It should not appear in failed or processed-not-indexed lists.
```

### 6. Confirm raw vector search

Use:

```http
POST /api/v1/ai/search
```

with a question containing an exact phrase from the document.

Example:

```json
{
  "question": "visitantes",
  "topK": 10,
  "similarityThreshold": 0.20
}
```

Expected:

```text
sourceCount > 0
sources include the expected document title/filename
excerpt contains the relevant text or nearby context
```

If `/search` works but `/chat` does not, the issue is planning/orchestration or answer generation.

If `/search` does not work, the issue is retrieval/indexing/filtering.

### 7. Test threshold and topK

Run the same query with:

```json
{
  "topK": 20,
  "similarityThreshold": 0.10
}
```

If results appear only with a lower threshold, tune `tamias.ai.default-similarity-threshold` or improve chunk quality.

### 8. Check property filtering

If the chat request includes `propertyId`, RAG may search only documents for that property, depending on the configured filter behavior.

Test both:

```json
{
  "question": "reglas de visitantes",
  "propertyId": null
}
```

and:

```json
{
  "question": "reglas de visitantes",
  "propertyId": "<property-id>"
}
```

If global search works but property search fails, the document may be linked to the wrong property or no property.

### 9. Check organization isolation

RAG must not leak documents across organizations. If the user belongs to a different organization than the document, retrieval should return nothing.

Verify the logged-in user and document share the same `organization_id`.

### 10. Check planner behavior

For document questions, the planner should choose:

```text
RAG_ONLY
RAG_FIRST
TOOL_AND_RAG
```

If a document question is planned as `TOOL_ONLY`, adjust `AiPlanningService` prompt/heuristics.

Use prompts such as:

```text
¿Qué dice el PDF sobre visitantes?
¿Qué reglas hay sobre basura?
¿Qué menciona el manual sobre filtros?
Según el documento, ¿qué no se puede hacer en la propiedad?
```

## Troubleshooting decision tree

```text
Question about PDF returns no answer
  |
  |-- Does document exist and download successfully?
  |      |-- No -> fix S3/storage metadata or re-upload document.
  |      |-- Yes
  |
  |-- Does /documents/{id}/chunks return readable chunks?
  |      |-- No -> fix processing/text extraction.
  |      |-- Yes
  |
  |-- Do chunks have vectorStoreId?
  |      |-- No -> re-run indexing.
  |      |-- Yes
  |
  |-- Does /api/v1/ai/search return sources with low threshold?
  |      |-- No -> check collection, organization/property filters, embedding/indexing.
  |      |-- Yes
  |
  |-- Does /api/v1/ai/chat return sources?
  |      |-- No -> check planner/orchestration path.
  |      |-- Yes -> answer generation issue if answer still says no info.
```

## Recommended manual SQL checks

Use these only in local/dev environments.

```sql
SELECT id, title, original_filename, s3_key, processing_status, status, property_id, organization_id
FROM documents
WHERE deleted_at IS NULL
ORDER BY created_at DESC;
```

```sql
SELECT document_id,
       COUNT(*) AS chunks,
       COUNT(vector_store_id) AS indexed_chunks,
       MIN(chunk_index) AS first_chunk,
       MAX(chunk_index) AS last_chunk
FROM document_chunks
GROUP BY document_id
ORDER BY chunks DESC;
```

```sql
SELECT d.title,
       d.original_filename,
       dc.chunk_index,
       LEFT(dc.content, 500) AS excerpt,
       dc.vector_store_collection,
       dc.vector_store_id
FROM document_chunks dc
JOIN documents d ON d.id = dc.document_id
WHERE LOWER(dc.content) LIKE LOWER('%visitantes%')
ORDER BY d.title, dc.chunk_index;
```

## When to re-upload documents

Re-upload is recommended when:

- The document was uploaded before the S3 provider was enabled.
- The `s3_key` points to an old/local storage convention.
- Download URL/file access fails.
- Processing succeeded before but chunks are empty or stale.
- Index status and Chroma state disagree.

## Expected final not-found message

When both system tools and RAG fail, the assistant should clearly say that both paths were checked.

Example:

```text
No encontré información relacionada con lo que preguntaste.

Revisé:
- Datos del sistema: no encontré registros suficientes.
- Documentos indexados/RAG: no encontré contenido relacionado.

Puedes intentar preguntar con otro nombre, revisar si el documento está procesado/indexado, o confirmar que la información exista en TAMIAS.
```
