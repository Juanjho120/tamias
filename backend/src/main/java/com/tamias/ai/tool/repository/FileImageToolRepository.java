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
public class FileImageToolRepository extends AiReadOnlyToolSupport {

    public FileImageToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer fileMetadata(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "file", "files", "metadata", "metadatos", "cargado", "cargados", "almacenado", "almacenados"
        ));
        List<Map<String, Object>> rows = fileMetadataRows(search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré archivos registrados en TAMIAS." : "No encontré archivos relacionados con “" + search + "”.",
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
        return AiToolAnswer.of(answer.toString(), "file.searchMetadata", "File metadata", "%d file metadata rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer filesByProperty(String userQuestion) {
        String propertySearch = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "asociado", "asociados", "propiedad", "propiedades", "para", "esta", "este"
        ));
        List<Map<String, Object>> rows = fileMetadataRows(null, propertySearch, null, DEFAULT_LIMIT);
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
        return AiToolAnswer.of(answer.toString(), "file.byProperty", "Files by property", "%d property file rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer filesByMaintenance(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "imagen", "imagenes", "foto", "fotos", "mantenimiento", "mantenimientos", "evidencia",
                "asociado", "asociados", "asociada", "asociadas", "relacionado", "relacionados"
        ));
        List<Map<String, Object>> rows = maintenanceImageRows(search, false, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré archivos o imágenes asociados a mantenimientos." : "No encontré archivos de mantenimiento relacionados con “" + search + "”.",
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
        return AiToolAnswer.of(answer.toString(), "file.byMaintenance", "Files by maintenance", "%d maintenance file rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer filesByDocument(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "archivo", "archivos", "documento", "documentos", "file", "metadata", "metadatos",
                "asociado", "asociados", "asociada", "asociadas", "relacionado", "relacionados"
        ));
        List<Map<String, Object>> rows = documentRows(search, "", q -> {}, DEFAULT_LIMIT, "d.created_at DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré archivos de documentos registrados." : "No encontré documentos relacionados con “" + search + "”.",
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
        return AiToolAnswer.of(answer.toString(), "file.byDocument", "Files by document", "%d document files found.".formatted(rows.size()), rows);
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
                      AND d.deleted_at IS NULL
                    UNION ALL
                    SELECT 'PROPERTY_IMAGE' AS source_type, pi.size_bytes
                    FROM property_images pi
                    WHERE pi.organization_id = :organizationId
                      AND pi.deleted_at IS NULL
                      AND pi.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'MAINTENANCE_IMAGE' AS source_type, mri.size_bytes
                    FROM maintenance_record_images mri
                    WHERE mri.organization_id = :organizationId
                      AND mri.deleted_at IS NULL
                      AND mri.status = 'ACTIVE'
                ) files
                GROUP BY source_type
                ORDER BY source_type ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "sourceType", "fileCount", "totalSizeBytes");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré archivos almacenados en TAMIAS.", "file.storageSummary", "File storage summary", "No file metadata found.", List.of());
        }
        long totalFiles = rows.stream().mapToLong(row -> toLong(row.get("fileCount"))).sum();
        long totalBytes = rows.stream().mapToLong(row -> toLong(row.get("totalSizeBytes"))).sum();
        StringBuilder answer = new StringBuilder("Tienes ").append(totalFiles).append(" archivos registrados en metadata, con ").append(formatBytes(totalBytes)).append(" aproximados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("sourceType"))))
                    .append(" | archivos: ").append(blankToDash(value(row.get("fileCount"))))
                    .append(" | tamaño: ").append(formatBytes(toLong(row.get("totalSizeBytes"))));
        }
        return AiToolAnswer.of(answer.toString(), "file.storageSummary", "File storage summary", "File metadata storage totals were calculated.", rows);
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
                  AND d.deleted_at IS NULL
                  AND d.property_id IS NULL
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "sourceType", "displayName", "originalFilename", "contentType", "sizeBytes", "status", "detailStatus", "propertyName", "createdAt");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré candidatos obvios de archivos huérfanos en la metadata actual. En esta fase solo reviso metadata registrada en documentos e imágenes; no hago auditoría directa del bucket S3.",
                    "file.orphanFileCandidates",
                    "Orphan file candidates",
                    "No obvious orphan file candidates found from metadata.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estos archivos podrían requerir revisión porque no están asociados a una propiedad:");
        appendFileMetadataRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "file.orphanFileCandidates", "Orphan file candidates", "%d candidate rows found.".formatted(rows.size()), rows);
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
                                            AND pi.deleted_at IS NULL
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
        return AiToolAnswer.of(answer.toString(), "image.propertyImagesSummary", "Property image metadata", "Property image metadata was summarized.", rows);
    }

    public AiToolAnswer maintenanceImageMetadataSummary() {
        List<Map<String, Object>> rows = maintenanceImageRows(null, false, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré imágenes asociadas a mantenimientos.", "image.maintenanceImagesSummary", "Maintenance image metadata", "No maintenance images found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas son las imágenes asociadas a mantenimientos:");
        appendMaintenanceImageRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "image.maintenanceImagesSummary", "Maintenance image metadata", "%d maintenance image rows found.".formatted(rows.size()), rows);
    }
}
