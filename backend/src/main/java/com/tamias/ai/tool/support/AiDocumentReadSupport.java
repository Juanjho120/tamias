package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AiDocumentReadSupport extends AiScheduledReservationReadSupport {

    protected AiDocumentReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> documentRows(String search, String extraWhere, QueryConfigurer extraConfigurer, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT d.id,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       d.status,
                       d.original_filename,
                       d.content_type,
                       d.size_bytes,
                       d.created_at,
                       COALESCE(p.name, '') AS property_name,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NULL AND dc.id IS NOT NULL THEN 1 ELSE 0 END), 0) AS missing_vector_id_count
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                """);
        if (extraWhere != null && !extraWhere.isBlank()) {
            sql.append(extraWhere).append(System.lineSeparator());
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', d.title, d.description, d.original_filename, d.document_type, d.processing_status, d.status, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY d.id, d.title, d.document_type, d.processing_status, d.status, d.original_filename, d.content_type, d.size_bytes, d.created_at, p.name
                """);
        sql.append("ORDER BY ").append(orderBy).append(System.lineSeparator());
        sql.append("LIMIT :limit");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null && !search.isBlank()) {
                q.setParameter("search", search);
            }
            if (extraConfigurer != null) {
                extraConfigurer.configure(q);
            }
            q.setParameter("limit", limit);
        }, "id", "title", "documentType", "processingStatus", "status", "originalFilename", "contentType", "sizeBytes", "createdAt", "propertyName", "chunkCount", "indexedChunkCount", "missingVectorIdCount");
    }

    protected AiToolAnswer documentRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, "Document metadata", "No document rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        appendDocumentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), toolName, "Document metadata", "%d document rows found.".formatted(rows.size()), rows);
    }

    protected void appendDocumentRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("documentType"))))
                    .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | chunks indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/").append(blankToDash(value(row.get("chunkCount"))));
        }
    }

    protected String documentTypeFilterFromQuestion(String normalized) {
        if (containsAny(normalized, "plano", "planos", "blueprint")) {
            return "IN ('BLUEPRINT', 'ELECTRICAL_PLAN', 'PLUMBING_PLAN', 'DRAINAGE_PLAN')";
        }
        if (containsAny(normalized, "regla", "reglas", "house rule", "senales", "señales")) {
            return "IN ('HOUSE_RULES', 'BATHROOM_RULES', 'PROPERTY_SIGNS')";
        }
        if (containsAny(normalized, "manual", "manuales")) {
            return "= 'MANUAL'";
        }
        return null;
    }
}
