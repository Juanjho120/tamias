package com.tamias.ai.tool.repository;

import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class DocumentRagToolRepository extends AiReadOnlyToolSupport {

    public DocumentRagToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer documentMetadata(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "documento", "documentos", "cargado", "cargados", "subido", "subidos", "procesado", "procesados", "registrado", "registrados"
        ));
        List<Map<String, Object>> rows = query("""
                SELECT d.id,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       d.status,
                       d.original_filename,
                       d.created_at,
                       p.name AS property_name,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(d.original_filename) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(d.document_type) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                GROUP BY d.id, d.title, d.document_type, d.processing_status, d.status, d.original_filename, d.created_at, p.name
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "title", "documentType", "processingStatus", "status", "originalFilename", "createdAt", "propertyName", "chunkCount", "indexedChunkCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré documentos cargados en tu organización."
                            : "No encontré documentos relacionados con “" + search + "”.",
                    "document.searchMetadata",
                    "Document metadata",
                    "No matching documents found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los documentos que encontré:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("documentType"))))
                    .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | chunks indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/").append(blankToDash(value(row.get("chunkCount"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "document.searchMetadata",
                "Document metadata",
                "%d documents found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer ragDocumentIndexStatus() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT d.id,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NULL AND dc.id IS NOT NULL THEN 1 ELSE 0 END), 0) AS missing_vector_id_count
                FROM documents d
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                GROUP BY d.id, d.title, d.document_type, d.processing_status
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "title", "documentType", "processingStatus", "chunkCount", "indexedChunkCount", "missingVectorIdCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré documentos para revisar el estado de indexación IA.",
                    "rag.documentIndexStatus",
                    "RAG document index status",
                    "No documents found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Así está el índice IA/RAG de tus documentos:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | chunks indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/").append(blankToDash(value(row.get("chunkCount"))))
                    .append(" | pendientes de vector: ").append(blankToDash(value(row.get("missingVectorIdCount"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "rag.documentIndexStatus",
                "RAG document index status",
                "%d document index statuses found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer documentByProperty(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion,
                "documento", "documentos", "propiedad", "para", "de", "la", "el"));
        List<Map<String, Object>> rows = documentRows(search, null, null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.byProperty",
                search == null ? "Estos son los documentos asociados a tus propiedades:" : "Estos son los documentos relacionados con “" + search + "”:",
                search == null ? "No encontré documentos asociados a propiedades." : "No encontré documentos relacionados con “" + search + "”.");
    }

    public AiToolAnswer documentByType(String userQuestion) {
        String normalized = normalize(userQuestion);
        String typeFilter = documentTypeFilterFromQuestion(normalized);
        if (typeFilter == null) {
            return documentMetadata(userQuestion);
        }
        List<Map<String, Object>> rows = documentRows(null, " AND d.document_type " + typeFilter + " ", null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.byType",
                "Estos son los documentos que encontré para ese tipo:",
                "No encontré documentos de ese tipo.");
    }

    public AiToolAnswer documentByStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "fallaron", "fallo", "failed", "error")) {
            return failedDocuments();
        }
        if (containsAny(normalized, "no proces", "sin proces", "pendiente", "pendientes", "unprocessed")) {
            return unprocessedDocuments();
        }
        if (containsAny(normalized, "procesados", "procesado", "processed")) {
            return processedDocuments();
        }
        if (containsAny(normalized, "indexados", "indexado")) {
            return indexedDocuments();
        }
        return documentMetadata(userQuestion);
    }

    public AiToolAnswer recentDocuments() {
        List<Map<String, Object>> rows = documentRows(null, null, null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.recent", "Estos son los documentos más recientes:", "No encontré documentos recientes.");
    }

    public AiToolAnswer unprocessedDocuments() {
        List<Map<String, Object>> rows = documentRows(null, " AND d.processing_status IN ('PENDING', 'PROCESSING') ", null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.unprocessed", "Estos documentos todavía no están completamente procesados:", "No encontré documentos pendientes o en procesamiento.");
    }

    public AiToolAnswer failedDocuments() {
        List<Map<String, Object>> rows = documentRows(null, " AND d.processing_status = 'FAILED' ", null, DEFAULT_LIMIT, "d.created_at DESC");
        return simpleDocumentRowsAnswer(rows, "document.failedProcessing", "Estos documentos fallaron al procesarse:", "No encontré información sobre documentos que fallaron al procesarse. Si necesitas más detalles o tienes otra consulta, no dudes en preguntar.");
    }

    public AiToolAnswer processedDocuments() {
        List<Map<String, Object>> rows = documentRows(null, " AND d.processing_status = 'PROCESSED' ", null, DEFAULT_LIMIT, "d.created_at DESC");
        return simpleDocumentRowsAnswer(rows, "document.processed", "Estos documentos ya fueron procesados:", "No encontré documentos procesados.");
    }

    public AiToolAnswer indexedDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND d.processing_status = 'PROCESSED' AND EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id AND dcx.vector_store_id IS NOT NULL) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return simpleDocumentRowsAnswer(rows, "document.indexed", "Estos documentos están listos para IA:", "No encontré documentos listos para IA.");
    }

    public AiToolAnswer notIndexedDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND NOT EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id AND dcx.vector_store_id IS NOT NULL) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.notIndexed", "Estos documentos todavía no están indexados para IA:", "No encontré documentos pendientes de indexación IA.");
    }

    public AiToolAnswer processedNotIndexedDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND d.processing_status = 'PROCESSED' AND NOT EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id AND dcx.vector_store_id IS NOT NULL) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return simpleDocumentRowsAnswer(rows, "document.processedNotIndexed", "Estos documentos están procesados pero no indexados para IA:", "No encontré información sobre documentos que están procesados pero no indexados para IA. Si necesitas más detalles o tienes otra consulta, no dudes en preguntar.");
    }

    public AiToolAnswer documentCountByType() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT d.document_type,
                       d.title,
                       d.processing_status,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM document_chunks dc
                           WHERE dc.document_id = d.id
                             AND dc.organization_id = d.organization_id
                             AND dc.vector_store_id IS NOT NULL
                       ) THEN TRUE ELSE FALSE END AS indexed
                FROM documents d
                WHERE d.organization_id = :organizationId
                ORDER BY d.document_type ASC, d.title ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "documentType", "title", "processingStatus", "indexed");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré documentos para agrupar por tipo.", "document.countByType", "Document count by type", "No documents found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Tienes los siguientes documentos agrupados por tipo:");
        appendDocumentsGroupedByType(answer, rows);
        return AiToolAnswer.of(answer.toString(), "document.countByType", "Document count by type", "%d documents grouped by type.".formatted(rows.size()), rows);
    }

    public AiToolAnswer documentCountByProperty() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(p.name, 'Sin propiedad') AS property_name,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                LEFT JOIN document_chunks dc ON dc.document_id = d.id AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                GROUP BY p.name, d.title, d.document_type, d.processing_status
                ORDER BY property_name ASC, d.document_type ASC, d.title ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "propertyName", "title", "documentType", "processingStatus", "chunkCount", "indexedChunkCount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré documentos para agrupar por propiedad.", "document.countByProperty", "Document count by property", "No documents found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son los documentos agrupados por propiedad:");
        appendDocumentsGroupedByProperty(answer, rows);
        return AiToolAnswer.of(answer.toString(), "document.countByProperty", "Document count by property", "%d documents grouped by property.".formatted(rows.size()), rows);
    }

    private AiToolAnswer simpleDocumentRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, "Document metadata", "No document rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("title"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Document metadata", "%d document rows found.".formatted(rows.size()), rows);
    }

    private void appendDocumentsGroupedByType(StringBuilder answer, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String type = blankToDash(value(row.get("documentType")));
            grouped.computeIfAbsent(type, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            answer.append(System.lineSeparator()).append(entry.getKey()).append(":");
            for (Map<String, Object> row : entry.getValue()) {
                answer.append(System.lineSeparator())
                        .append("- ")
                        .append(blankToDash(value(row.get("title"))));
            }
        }
    }

    private void appendDocumentsGroupedByProperty(StringBuilder answer, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String propertyName = blankToDash(value(row.get("propertyName")));
            grouped.computeIfAbsent(propertyName, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            answer.append(System.lineSeparator()).append(System.lineSeparator()).append(entry.getKey());
            Map<String, List<Map<String, Object>>> byType = new LinkedHashMap<>();
            for (Map<String, Object> row : entry.getValue()) {
                String type = blankToDash(value(row.get("documentType")));
                byType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(row);
            }
            for (Map.Entry<String, List<Map<String, Object>>> typeEntry : byType.entrySet()) {
                answer.append(System.lineSeparator()).append("- ").append(typeEntry.getKey());
                for (Map<String, Object> row : typeEntry.getValue()) {
                    answer.append(System.lineSeparator())
                            .append("  - ")
                            .append(blankToDash(value(row.get("title"))))
                            .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                            .append(" | chunks indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                            .append("/").append(blankToDash(value(row.get("chunkCount"))));
                }
            }
        }
    }

    public AiToolAnswer findBlueprintDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND (d.document_type IN ('BLUEPRINT', 'ELECTRICAL_PLAN', 'PLUMBING_PLAN', 'DRAINAGE_PLAN') OR translate(LOWER(CONCAT_WS(' ', d.title, d.description, d.original_filename)), 'áéíóúüñ', 'aeiouun') LIKE '%plano%') ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.findBlueprints", "Estos son los planos o documentos técnicos que encontré:", "No encontré planos cargados.");
    }

    public AiToolAnswer findHouseRulesDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND (d.document_type IN ('HOUSE_RULES', 'BATHROOM_RULES', 'PROPERTY_SIGNS') OR translate(LOWER(CONCAT_WS(' ', d.title, d.description, d.original_filename)), 'áéíóúüñ', 'aeiouun') LIKE '%regla%') ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.findHouseRules", "Estas son las reglas o señalizaciones cargadas:", "No encontré reglas de casa cargadas.");
    }

    public AiToolAnswer findManualDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND (d.document_type = 'MANUAL' OR translate(LOWER(CONCAT_WS(' ', d.title, d.description, d.original_filename)), 'áéíóúüñ', 'aeiouun') LIKE '%manual%') ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.findManuals", "Estos son los manuales que encontré:", "No encontré manuales cargados.");
    }

    public AiToolAnswer ragChunkSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT d.id,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count,
                       COALESCE(SUM(COALESCE(dc.token_count, 0)), 0) AS token_count,
                       COALESCE(MIN(dc.chunk_index), 0) AS first_chunk_index,
                       COALESCE(MAX(dc.chunk_index), 0) AS last_chunk_index
                FROM documents d
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                GROUP BY d.id, d.title, d.document_type, d.processing_status
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "title", "documentType", "processingStatus", "chunkCount", "indexedChunkCount", "tokenCount", "firstChunkIndex", "lastChunkIndex");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré chunks de documentos para resumir.", "rag.chunkSummary", "RAG chunk summary", "No document chunk rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Este es el resumen de chunks por documento:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | chunks: ").append(blankToDash(value(row.get("chunkCount"))))
                    .append(" | indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append(" | tokens: ").append(blankToDash(value(row.get("tokenCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "rag.chunkSummary", "RAG chunk summary", "%d chunk summary rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer documentsMissingChunks() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND NOT EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "rag.documentsMissingChunks", "Estos documentos no tienen chunks generados:", "No encontré documentos sin chunks.");
    }

    public AiToolAnswer documentsMissingVectorIds() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id AND dcx.vector_store_id IS NULL) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "rag.documentsMissingVectorIds", "Estos documentos tienen chunks pendientes de vector_store_id:", "No encontré documentos con chunks pendientes de vector_store_id.");
    }

    public AiToolAnswer ragIndexCoverageSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT d.id) AS document_count,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count,
                       COALESCE(SUM(CASE WHEN dc.id IS NOT NULL AND dc.vector_store_id IS NULL THEN 1 ELSE 0 END), 0) AS missing_vector_id_count,
                       COUNT(DISTINCT CASE WHEN dc.id IS NULL THEN d.id END) AS documents_missing_chunks,
                       COUNT(DISTINCT CASE WHEN dc.vector_store_id IS NOT NULL THEN d.id END) AS documents_with_indexed_chunks
                FROM documents d
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                """, q -> q.setParameter("organizationId", organizationId),
                "documentCount", "chunkCount", "indexedChunkCount", "missingVectorIdCount", "documentsMissingChunks", "documentsWithIndexedChunks");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Cobertura actual del índice IA/RAG:" + System.lineSeparator()
                + "- Documentos: " + blankToDash(value(row.get("documentCount"))) + System.lineSeparator()
                + "- Chunks generados: " + blankToDash(value(row.get("chunkCount"))) + System.lineSeparator()
                + "- Chunks con vector_store_id: " + blankToDash(value(row.get("indexedChunkCount"))) + System.lineSeparator()
                + "- Chunks pendientes de vector_store_id: " + blankToDash(value(row.get("missingVectorIdCount"))) + System.lineSeparator()
                + "- Documentos sin chunks: " + blankToDash(value(row.get("documentsMissingChunks"))) + System.lineSeparator()
                + "- Documentos con al menos un chunk indexado: " + blankToDash(value(row.get("documentsWithIndexedChunks")));
        return AiToolAnswer.of(answer, "rag.indexCoverageSummary", "RAG index coverage summary", "RAG index coverage summary was calculated.", rows);
    }
}
