# Phase 9L — Document metadata and RAG health AI tools

## Scope

This phase expands TAMIAS AI Tool Calling with read-only document metadata and RAG health tools.

The assistant still does not read or expose private S3 URLs through these tools. Document content questions continue to use the existing RAG flow. These tools only summarize operational metadata stored in PostgreSQL.

## Implemented tools

### Document metadata tools

- `document.searchMetadata`
- `document.byProperty`
- `document.byType`
- `document.byStatus`
- `document.recent`
- `document.unprocessed`
- `document.failedProcessing`
- `document.processed`
- `document.indexed`
- `document.notIndexed`
- `document.countByType`
- `document.countByProperty`
- `document.findBlueprints`
- `document.findHouseRules`
- `document.findManuals`

### RAG health tools

- `rag.documentIndexStatus`
- `rag.chunkSummary`
- `rag.documentsMissingChunks`
- `rag.documentsMissingVectorIds`
- `rag.indexCoverageSummary`

## Example prompts

- ¿Qué documentos tengo para esta propiedad?
- ¿Qué documentos no han sido procesados?
- ¿Qué documentos fallaron al procesarse?
- ¿Qué planos tengo cargados?
- ¿Qué reglas de casa están indexadas para IA?
- ¿Qué documentos están listos para IA?
- ¿Qué documentos tienen chunks pero no vector_store_id?
- ¿Hay documentos procesados que no estén indexados?
- Dame un resumen de chunks.
- Dame la cobertura del índice RAG.

## Security model

- Read-only first.
- No free SQL from the model.
- Backend-owned `organization_id` from `CurrentUserService`.
- No autonomous writes.
- No private S3 URL exposure.
- Content search remains RAG-based; these tools only expose metadata and index health.

## Notes

Document types are aligned with the current enum values:

- `HOUSE_RULES`
- `BATHROOM_RULES`
- `PROPERTY_SIGNS`
- `BLUEPRINT`
- `ELECTRICAL_PLAN`
- `PLUMBING_PLAN`
- `DRAINAGE_PLAN`
- `MANUAL`
- `OTHER`

Processing statuses are aligned with the current enum values:

- `PENDING`
- `PROCESSING`
- `PROCESSED`
- `FAILED`
