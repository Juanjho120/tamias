package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class FileImageToolRepository extends AiReadOnlyToolSupport {

    public FileImageToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer fileMetadata(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "file", "files", "metadata", "metadatos",
                "cargado", "cargados", "almacenado", "almacenados"
        ));

        List<Map<String, Object>> rows = fileMetadataRows(search, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré archivos registrados en TAMIAS."
                            : "No encontré archivos relacionados con “" + search + "”.",
                    "file.searchMetadata",
                    "File metadata",
                    "No file metadata found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estos son los archivos que encontré en TAMIAS:"
                : "Estos son los archivos que encontré relacionados con “" + search + "”:");
        appendFileMetadataRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "file.searchMetadata",
                "File metadata",
                "%d file metadata rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer filesByProperty(String userQuestion) {
        String propertySearch = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "asociado", "asociados", "propiedad", "propiedades",
                "para", "esta", "este"
        ));

        List<Map<String, Object>> rows = fileMetadataRows(null, propertySearch, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    propertySearch == null
                            ? "No encontré archivos asociados a propiedades."
                            : "No encontré archivos asociados a una propiedad que coincida con “" + propertySearch + "”.",
                    "file.byProperty",
                    "Files by property",
                    "No files found for the requested property.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(propertySearch == null
                ? "Estos archivos están asociados a propiedades:"
                : "Estos archivos están asociados a propiedades relacionadas con “" + propertySearch + "”:");
        appendFileMetadataRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "file.byProperty",
                "Files by property",
                "%d property file rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer filesByMaintenance(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "imagen", "imagenes", "foto", "fotos", "mantenimiento",
                "mantenimientos", "evidencia", "asociado", "asociados", "asociada", "asociadas",
                "relacionado", "relacionados"
        ));

        List<Map<String, Object>> rows = maintenanceImageRows(search, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré archivos o imágenes asociados a mantenimientos."
                            : "No encontré archivos de mantenimiento relacionados con “" + search + "”.",
                    "file.byMaintenance",
                    "Files by maintenance",
                    "No maintenance files found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estos archivos están asociados a mantenimientos:"
                : "Estos archivos de mantenimiento están relacionados con “" + search + "”:");
        appendMaintenanceImageRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "file.byMaintenance",
                "Files by maintenance",
                "%d maintenance file rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer filesByDocument(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "documento", "documentos", "file", "metadata", "metadatos",
                "asociado", "asociados", "asociada", "asociadas", "relacionado", "relacionados"
        ));

        List<Map<String, Object>> rows = documentFileRows(search, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré archivos de documentos registrados."
                            : "No encontré documentos relacionados con “" + search + "”.",
                    "file.byDocument",
                    "Files by document",
                    "No document files found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estos son los archivos de documentos registrados:"
                : "Estos son los documentos relacionados con “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | archivo: ").append(blankToDash(value(row.get("originalFilename"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("documentType"))))
                    .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "file.byDocument",
                "Files by document",
                "%d document files found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer fileStorageSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT source_type,
                       COUNT(*) AS file_count,
                       COALESCE(SUM(size_bytes), 0) AS total_size_bytes
                FROM (
                    SELECT 'DOCUMENT' AS source_type, d.size_bytes
                    FROM documents d
                    WHERE d.organization_id = :organizationId
                        UNION ALL
                    SELECT 'PROPERTY_IMAGE' AS source_type, pi.size_bytes
                    FROM property_images pi
                    WHERE pi.organization_id = :organizationId
                      AND pi.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'MAINTENANCE_IMAGE' AS source_type, mri.size_bytes
                    FROM maintenance_record_images mri
                    WHERE mri.organization_id = :organizationId
                      AND mri.status = 'ACTIVE'
                ) files
                GROUP BY source_type
                ORDER BY source_type ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "sourceType", "fileCount", "totalSizeBytes");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré archivos almacenados en TAMIAS.",
                    "file.storageSummary",
                    "File storage summary",
                    "No file metadata found.",
                    List.of()
            );
        }

        long totalFiles = rows.stream().mapToLong(row -> toLong(row.get("fileCount"))).sum();
        long totalBytes = rows.stream().mapToLong(row -> toLong(row.get("totalSizeBytes"))).sum();

        StringBuilder answer = new StringBuilder("Tienes ")
                .append(totalFiles)
                .append(" archivos registrados en metadata, con ")
                .append(formatBytes(totalBytes))
                .append(" aproximados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("sourceType"))))
                    .append(" | archivos: ").append(blankToDash(value(row.get("fileCount"))))
                    .append(" | tamaño: ").append(formatBytes(toLong(row.get("totalSizeBytes"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "file.storageSummary",
                "File storage summary",
                "File metadata storage totals were calculated.",
                rows
        );
    }

    public AiToolAnswer orphanFileCandidates() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT 'DOCUMENT_WITHOUT_PROPERTY' AS source_type,
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
                  AND d.property_id IS NULL
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "sourceType", "displayName", "originalFilename", "contentType", "sizeBytes", "status", "detailStatus", "propertyName", "createdAt");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré candidatos obvios de archivos huérfanos en la metadata actual.\n"
                            + "En esta fase solo reviso metadata registrada en documentos e imágenes; no hago auditoría directa del bucket S3.",
                    "file.orphanFileCandidates",
                    "Orphan file candidates",
                    "No obvious orphan file candidates found from metadata.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos archivos podrían requerir revisión porque no están asociados a una propiedad:");
        appendFileMetadataRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "file.orphanFileCandidates",
                "Orphan file candidates",
                "%d candidate rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer propertyImageMetadataSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT p.name AS property_name,
                       COUNT(pi.id) AS image_count,
                       COALESCE(SUM(CASE WHEN pi.is_cover = TRUE THEN 1 ELSE 0 END), 0) AS cover_count,
                       COALESCE(SUM(pi.size_bytes), 0) AS total_size_bytes,
                       COALESCE(STRING_AGG(pi.original_filename, ', ' ORDER BY pi.is_cover DESC, pi.created_at DESC), '') AS filenames
                FROM properties p
                LEFT JOIN property_images pi ON pi.property_id = p.id
                    AND pi.organization_id = p.organization_id
                    AND pi.status = 'ACTIVE'
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name
                ORDER BY image_count DESC, p.name ASC
                LIMIT :limit
                """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "propertyName", "imageCount", "coverCount", "totalSizeBytes", "filenames");

        StringBuilder answer = new StringBuilder("Resumen de imágenes por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))))
                    .append(" | portadas: ").append(blankToDash(value(row.get("coverCount"))))
                    .append(" | tamaño: ").append(formatBytes(toLong(row.get("totalSizeBytes"))));

            String filenames = value(row.get("filenames"));
            if (!filenames.isBlank()) {
                answer.append(" | archivos: ").append(filenames);
            }
        }

        return AiToolAnswer.of(
                answer.toString(),
                "image.propertyImagesSummary",
                "Property image metadata",
                "Property image metadata was summarized.",
                rows
        );
    }

    public AiToolAnswer maintenanceImageMetadataSummary() {
        List<Map<String, Object>> rows = maintenanceImageRows(null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré imágenes asociadas a mantenimientos.",
                    "image.maintenanceImagesSummary",
                    "Maintenance image metadata",
                    "No maintenance images found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas son las imágenes asociadas a mantenimientos:");
        appendMaintenanceImageRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "image.maintenanceImagesSummary",
                "Maintenance image metadata",
                "%d maintenance image rows found.".formatted(rows.size()),
                rows
        );
    }

    private List<Map<String, Object>> fileMetadataRows(String search, String propertySearch, int limit) {
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
                           p.name AS display_name,
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
                           mr.title AS display_name,
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

        if (search != null) {
            sql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', source_type, display_name, original_filename, content_type, status, detail_status, property_name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(property_name), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        sql.append("ORDER BY created_at DESC LIMIT :limit");

        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            if (propertySearch != null) {
                q.setParameter("propertySearch", propertySearch);
            }
            q.setParameter("limit", limit);
        }, "sourceType", "displayName", "originalFilename", "contentType", "sizeBytes", "status", "detailStatus", "propertyName", "createdAt");
    }

    private List<Map<String, Object>> documentFileRows(String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT d.title,
                       d.original_filename,
                       d.document_type,
                       d.processing_status,
                       COALESCE(p.name, 'Sin propiedad') AS property_name,
                       d.created_at
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id
                    AND p.organization_id = d.organization_id
                    AND p.deleted_at IS NULL
                WHERE d.organization_id = :organizationId
                """);
        if (search != null) {
            sql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', d.title, d.original_filename, d.document_type, d.processing_status, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        sql.append("ORDER BY d.created_at DESC LIMIT :limit");

        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "title", "originalFilename", "documentType", "processingStatus", "propertyName", "createdAt");
    }

    private List<Map<String, Object>> maintenanceImageRows(String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT mr.title,
                       p.name AS property_name,
                       COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS maintenance_date,
                       COUNT(mri.id) AS image_count,
                       COALESCE(STRING_AGG(mri.original_filename, ', ' ORDER BY mri.created_at DESC), '') AS filenames
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                    AND p.organization_id = mr.organization_id
                    AND p.deleted_at IS NULL
                JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                    AND mri.organization_id = mr.organization_id
                    AND mri.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', mr.title, p.name, mr.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        sql.append("""
                GROUP BY mr.id, mr.title, p.name, mr.performed_at, mr.scheduled_at, mr.created_at
                ORDER BY maintenance_date DESC
                LIMIT :limit
                """);

        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "title", "propertyName", "maintenanceDate", "imageCount", "filenames");
    }
}
