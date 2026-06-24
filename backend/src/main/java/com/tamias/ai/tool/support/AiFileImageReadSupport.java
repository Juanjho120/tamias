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

public abstract class AiFileImageReadSupport extends AiPurchaseReadSupport {

    protected AiFileImageReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> fileMetadataRows(String search, String propertySearch, String sourceType, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM (
                    SELECT 'DOCUMENT' AS source_type,
                           d.title AS display_name,
                           d.original_filename,
                           d.content_type,
                           d.size_bytes,
                           d.status,
                           d.processing_status AS detail_status,
                           COALESCE(p.name, 'Sin propiedad') AS property_name,
                           d.created_at
                    FROM documents d
                    LEFT JOIN properties p ON p.id = d.property_id
                                      AND p.organization_id = d.organization_id
                                      AND p.deleted_at IS NULL
                    WHERE d.organization_id = :organizationId
                        UNION ALL
                    SELECT 'PROPERTY_IMAGE' AS source_type,
                           pi.original_filename AS display_name,
                           pi.original_filename,
                           pi.content_type,
                           pi.size_bytes,
                           pi.status,
                           CASE WHEN pi.is_cover = TRUE THEN 'COVER' ELSE 'IMAGE' END AS detail_status,
                           p.name AS property_name,
                           pi.created_at
                    FROM property_images pi
                    JOIN properties p ON p.id = pi.property_id
                                     AND p.organization_id = pi.organization_id
                                     AND p.deleted_at IS NULL
                    WHERE pi.organization_id = :organizationId
                          AND pi.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'MAINTENANCE_IMAGE' AS source_type,
                           mri.original_filename AS display_name,
                           mri.original_filename,
                           mri.content_type,
                           mri.size_bytes,
                           mri.status,
                           mr.status AS detail_status,
                           p.name AS property_name,
                           mri.created_at
                    FROM maintenance_record_images mri
                    JOIN maintenance_records mr ON mr.id = mri.maintenance_record_id
                                               AND mr.organization_id = mri.organization_id
                                               AND mr.deleted_at IS NULL
                    JOIN properties p ON p.id = mr.property_id
                                     AND p.organization_id = mr.organization_id
                                     AND p.deleted_at IS NULL
                    WHERE mri.organization_id = :organizationId
                          AND mri.status = 'ACTIVE'
                ) files
                WHERE 1 = 1
                """);
        if (sourceType != null) {
            sql.append("  AND source_type = :sourceType\n");
        }
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', display_name, original_filename, content_type, detail_status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(property_name), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append("ORDER BY created_at DESC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (sourceType != null) q.setParameter("sourceType", sourceType);
            if (search != null) q.setParameter("search", search);
            if (propertySearch != null) q.setParameter("propertySearch", propertySearch);
            q.setParameter("limit", limit);
        }, "sourceType", "displayName", "originalFilename", "contentType", "sizeBytes", "status", "detailStatus", "propertyName", "createdAt");
    }

    protected List<Map<String, Object>> maintenanceImageRows(String search, boolean withoutImages, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT mr.id,
                       mr.title,
                       p.name AS property_name,
                       COALESCE(mr.performed_at, mr.scheduled_at) AS maintenance_date,
                       COUNT(mri.id) AS image_count,
                       COALESCE(STRING_AGG(mri.original_filename, ', ' ORDER BY mri.created_at DESC), '') AS filenames
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                                 AND p.organization_id = mr.organization_id
                LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                                                       AND mri.organization_id = mr.organization_id
                                                                                            AND mri.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', mr.title, mr.description, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("GROUP BY mr.id, mr.title, p.name, mr.performed_at, mr.scheduled_at\n");
        sql.append(withoutImages ? "HAVING COUNT(mri.id) = 0\n" : "HAVING COUNT(mri.id) > 0\n");
        sql.append("ORDER BY COALESCE(mr.performed_at, mr.scheduled_at) DESC NULLS LAST, mr.title ASC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "title", "propertyName", "maintenanceDate", "imageCount", "filenames");
    }

    protected void appendFileMetadataRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("displayName"))))
                    .append(" | origen: ").append(blankToDash(value(row.get("sourceType"))))
                    .append(" | archivo: ").append(blankToDash(value(row.get("originalFilename"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("contentType"))))
                    .append(" | tamaño: ").append(formatBytes(toLong(row.get("sizeBytes"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
        }
    }

    protected void appendMaintenanceImageRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | fecha: ").append(formatDateTime(row.get("maintenanceDate")))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
            String filenames = value(row.get("filenames"));
            if (!filenames.isBlank()) {
                for (String filename : splitCommaValues(filenames)) {
                    answer.append(System.lineSeparator())
                            .append("	- ").append(filename);
                }
            }
        }
    }
}
