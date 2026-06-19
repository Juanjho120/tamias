package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
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
                    UNION ALL
                    SELECT 'INVENTORY_ITEM_IMAGE' AS source_type, iii.size_bytes
                    FROM inventory_item_images iii
                    WHERE iii.organization_id = :organizationId
                      AND iii.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'PURCHASE_IMAGE' AS source_type, pui.size_bytes
                    FROM purchase_images pui
                    WHERE pui.organization_id = :organizationId
                      AND pui.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'RESERVATION_IMAGE' AS source_type, ri.size_bytes
                    FROM reservation_images ri
                    WHERE ri.organization_id = :organizationId
                      AND ri.status = 'ACTIVE'
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


    public AiToolAnswer imageDashboardSummary() {
        List<Map<String, Object>> rows = imageDashboardRows();
        long totalImages = rows.stream().mapToLong(row -> toLong(row.get("imageCount"))).sum();
        long totalBytes = rows.stream().mapToLong(row -> toLong(row.get("totalSizeBytes"))).sum();
        if (totalImages == 0) {
            return AiToolAnswer.of(
                    "No encontré imágenes registradas en TAMIAS para tu organización.",
                    "files.getImageDashboardSummary",
                    "Image dashboard summary",
                    "No active image metadata was found.",
                    rows
            );
        }

        StringBuilder answer = new StringBuilder("Resumen de imágenes en TAMIAS:")
                .append(System.lineSeparator())
                .append("- Total de imágenes: ").append(totalImages)
                .append(System.lineSeparator())
                .append("- Storage estimado: ").append(formatBytes(totalBytes));
        appendImageModuleRows(answer, rows, true);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getImageDashboardSummary",
                "Image dashboard summary",
                "Image counts by module were calculated.",
                rows
        );
    }

    public AiToolAnswer imageCountByModule() {
        List<Map<String, Object>> rows = imageDashboardRows();
        long totalImages = rows.stream().mapToLong(row -> toLong(row.get("imageCount"))).sum();
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré imágenes registradas en TAMIAS para tu organización.",
                    "files.getImageCountByModule",
                    "Image count by module",
                    "No image module metadata was found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Total de imágenes en TAMIAS por módulo:");
        appendImageModuleRows(answer, rows, false);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getImageCountByModule",
                "Image count by module",
                "%d image module rows found with %d total images.".formatted(rows.size(), totalImages),
                rows
        );
    }

    public AiToolAnswer topImageModule() {
        List<Map<String, Object>> rows = imageDashboardRows();
        long totalImages = rows.stream().mapToLong(row -> toLong(row.get("imageCount"))).sum();
        if (totalImages == 0 || rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré imágenes registradas en TAMIAS para tu organización.",
                    "files.getTopImageModule",
                    "Top image module",
                    "No active image metadata was found.",
                    rows
            );
        }

        Map<String, Object> topModule = rows.get(0);
        long topImageCount = toLong(topModule.get("imageCount"));
        String answer = "El módulo con más imágenes es "
                + blankToDash(value(topModule.get("moduleLabel")))
                + ", con "
                + topImageCount
                + (topImageCount == 1 ? " imagen registrada." : " imágenes registradas.");

        return AiToolAnswer.of(
                answer,
                "files.getTopImageModule",
                "Top image module",
                "The module with the most images was selected.",
                rows
        );
    }

    public AiToolAnswer imageStorageSummary() {
        List<Map<String, Object>> rows = imageDashboardRows();
        long totalImages = rows.stream().mapToLong(row -> toLong(row.get("imageCount"))).sum();
        long totalBytes = rows.stream().mapToLong(row -> toLong(row.get("totalSizeBytes"))).sum();
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré storage registrado para imágenes en TAMIAS.",
                    "files.getImageStorageSummary",
                    "Image storage summary",
                    "No image storage metadata was found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Resumen de storage en TAMIAS:")
                .append(System.lineSeparator())
                .append("- Total de imágenes: ").append(totalImages)
                .append(System.lineSeparator())
                .append("- Storage estimado: ").append(formatBytes(totalBytes));
        appendImageModuleRows(answer, rows, false);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getImageStorageSummary",
                "Image storage summary",
                "Image storage by module was calculated.",
                rows
        );
    }

    public AiToolAnswer fileNameList() {
        List<Map<String, Object>> rows = fileMetadataRows(null, null, 50, "source_type ASC, display_name ASC, original_filename ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré archivos registrados en TAMIAS.",
                    "files.getFileNameList",
                    "File name list",
                    "No file metadata found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los archivos registrados en TAMIAS:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("originalFilename"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "files.getFileNameList",
                "File name list",
                "%d file names found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer recentUploads() {
        List<Map<String, Object>> rows = fileMetadataRows(null, null, DEFAULT_LIMIT, "created_at DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré documentos ni imágenes subidas recientemente en TAMIAS.",
                    "files.getRecentUploads",
                    "Recent uploads",
                    "No recent upload metadata was found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los documentos o imágenes subidos recientemente:");
        appendGroupedFileMetadataRows(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getRecentUploads",
                "Recent uploads",
                "%d recent upload rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer largestFiles() {
        List<Map<String, Object>> rows = fileMetadataRows(null, null, DEFAULT_LIMIT, "size_bytes DESC NULLS LAST, created_at DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré documentos ni imágenes con metadata de tamaño en TAMIAS.",
                    "files.getLargestFiles",
                    "Largest files",
                    "No file size metadata was found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los archivos más grandes registrados:");
        appendLargestFileRowsGrouped(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getLargestFiles",
                "Largest files",
                "%d largest file rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer entitiesWithMostImages() {
        List<Map<String, Object>> rows = entityImageCountRows(false, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré entidades con imágenes registradas en TAMIAS.",
                    "files.getEntitiesWithMostImages",
                    "Entities with most images",
                    "No entities with images were found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas entidades tienen más imágenes registradas:");
        appendEntityImageCountRows(answer, rows, false);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getEntitiesWithMostImages",
                "Entities with most images",
                "%d entities with image counts found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer entitiesWithoutImages() {
        List<Map<String, Object>> rows = entityImageCountRows(true, 5);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré entidades sin imágenes registradas en TAMIAS.",
                    "files.getEntitiesWithoutImages",
                    "Entities without images",
                    "No entities without images were found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas entidades no tienen imágenes registradas:");
        appendEntityImageCountRows(answer, rows, true);

        return AiToolAnswer.of(
                answer.toString(),
                "files.getEntitiesWithoutImages",
                "Entities without images",
                "%d entities without image rows found.".formatted(rows.size()),
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
                    "No encontré información sobre mantenimientos que tengan imágenes. Si necesitas más detalles o tienes otra consulta, no dudes en preguntar.",
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


    private List<Map<String, Object>> imageDashboardRows() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return query("""
                SELECT module_key,
                       module_label,
                       image_count,
                       total_size_bytes,
                       entity_count,
                       entities_with_images,
                       entity_count - entities_with_images AS entities_without_images
                FROM (
                    SELECT 'properties' AS module_key,
                           'Propiedades' AS module_label,
                           COUNT(pi.id) AS image_count,
                           COALESCE(SUM(pi.size_bytes), 0) AS total_size_bytes,
                           COUNT(DISTINCT p.id) AS entity_count,
                           COUNT(DISTINCT CASE WHEN pi.id IS NOT NULL THEN p.id END) AS entities_with_images
                    FROM properties p
                    LEFT JOIN property_images pi ON pi.property_id = p.id
                        AND pi.organization_id = p.organization_id
                        AND pi.status = 'ACTIVE'
                    WHERE p.organization_id = :organizationId
                      AND p.deleted_at IS NULL
                    UNION ALL
                    SELECT 'maintenance' AS module_key,
                           'Mantenimientos' AS module_label,
                           COUNT(mri.id) AS image_count,
                           COALESCE(SUM(mri.size_bytes), 0) AS total_size_bytes,
                           COUNT(DISTINCT mr.id) AS entity_count,
                           COUNT(DISTINCT CASE WHEN mri.id IS NOT NULL THEN mr.id END) AS entities_with_images
                    FROM maintenance_records mr
                    LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                        AND mri.organization_id = mr.organization_id
                        AND mri.status = 'ACTIVE'
                    WHERE mr.organization_id = :organizationId
                      AND mr.deleted_at IS NULL
                    UNION ALL
                    SELECT 'inventory_items' AS module_key,
                           'Items de inventario' AS module_label,
                           COUNT(iii.id) AS image_count,
                           COALESCE(SUM(iii.size_bytes), 0) AS total_size_bytes,
                           COUNT(DISTINCT ii.id) AS entity_count,
                           COUNT(DISTINCT CASE WHEN iii.id IS NOT NULL THEN ii.id END) AS entities_with_images
                    FROM inventory_items ii
                    LEFT JOIN inventory_item_images iii ON iii.inventory_item_id = ii.id
                        AND iii.organization_id = ii.organization_id
                        AND iii.status = 'ACTIVE'
                    WHERE ii.organization_id = :organizationId
                      AND ii.deleted_at IS NULL
                    UNION ALL
                    SELECT 'purchase_lists' AS module_key,
                           'Listas de compra' AS module_label,
                           COUNT(pui.id) AS image_count,
                           COALESCE(SUM(pui.size_bytes), 0) AS total_size_bytes,
                           COUNT(DISTINCT pl.id) AS entity_count,
                           COUNT(DISTINCT CASE WHEN pui.id IS NOT NULL THEN pl.id END) AS entities_with_images
                    FROM purchase_lists pl
                    LEFT JOIN purchase_images pui ON pui.purchase_list_id = pl.id
                        AND pui.organization_id = pl.organization_id
                        AND pui.status = 'ACTIVE'
                    WHERE pl.organization_id = :organizationId
                      AND pl.deleted_at IS NULL
                    UNION ALL
                    SELECT 'reservations' AS module_key,
                           'Reservaciones' AS module_label,
                           COUNT(ri.id) AS image_count,
                           COALESCE(SUM(ri.size_bytes), 0) AS total_size_bytes,
                           COUNT(DISTINCT r.id) AS entity_count,
                           COUNT(DISTINCT CASE WHEN ri.id IS NOT NULL THEN r.id END) AS entities_with_images
                    FROM reservations r
                    LEFT JOIN reservation_images ri ON ri.reservation_id = r.id
                        AND ri.organization_id = r.organization_id
                        AND ri.status = 'ACTIVE'
                    WHERE r.organization_id = :organizationId
                      AND r.deleted_at IS NULL
                ) summary
                ORDER BY image_count DESC, module_label ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "moduleKey", "moduleLabel", "imageCount", "totalSizeBytes", "entityCount", "entitiesWithImages", "entitiesWithoutImages");
    }

    private void appendImageModuleRows(StringBuilder answer, List<Map<String, Object>> rows, boolean includeEntityCounts) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("moduleLabel"))))
                    .append(": ")
                    .append(blankToDash(value(row.get("imageCount"))))
                    .append(toLong(row.get("imageCount")) == 1 ? " imagen" : " imágenes");
            if (includeEntityCounts) {
                answer.append(" | entidades con imágenes: ")
                        .append(blankToDash(value(row.get("entitiesWithImages"))))
                        .append(" | sin imágenes: ")
                        .append(blankToDash(value(row.get("entitiesWithoutImages"))));
            }
            answer.append(" | tamaño: ")
                    .append(formatBytes(toLong(row.get("totalSizeBytes"))));
        }
    }

    private void appendLargestFileRowsGrouped(StringBuilder answer, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> groupedRows = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = value(row.get("displayName"))
                    + "\u0001" + value(row.get("sourceType"))
                    + "\u0001" + value(row.get("propertyName"));
            groupedRows.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(row);
        }

        for (List<Map<String, Object>> group : groupedRows.values()) {
            Map<String, Object> first = group.get(0);
            answer.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(first.get("displayName"))))
                    .append(System.lineSeparator())
                    .append(blankToDash(value(first.get("sourceType"))));
            for (Map<String, Object> row : group) {
                answer.append(System.lineSeparator())
                        .append("  - archivo: ")
                        .append(blankToDash(value(row.get("originalFilename"))))
                        .append(" | tipo: ")
                        .append(blankToDash(value(row.get("contentType"))))
                        .append(" | tamaño: ")
                        .append(formatBytes(toLong(row.get("sizeBytes"))));
            }
        }
    }

    private List<Map<String, Object>> entityImageCountRows(boolean withoutImages, int limitPerModule) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String havingClause = withoutImages ? "WHERE image_count = 0" : "WHERE image_count > 0";
        String ordering = withoutImages
                ? "module_order ASC, module_label ASC, module_rank ASC"
                : "module_order ASC, image_count DESC, total_size_bytes DESC, entity_label ASC";
        String sql = """
                WITH candidates AS (
                    SELECT 1 AS module_order,
                           'properties' AS module_key,
                           'Propiedades' AS module_label,
                           p.id AS entity_id,
                           p.name AS entity_label,
                           '' AS context_detail,
                           COUNT(pi.id) AS image_count,
                           COALESCE(SUM(pi.size_bytes), 0) AS total_size_bytes,
                           MAX(pi.created_at) AS last_image_at,
                           p.created_at AS sort_date
                    FROM properties p
                    LEFT JOIN property_images pi ON pi.property_id = p.id
                        AND pi.organization_id = p.organization_id
                        AND pi.status = 'ACTIVE'
                    WHERE p.organization_id = :organizationId
                      AND p.deleted_at IS NULL
                    GROUP BY p.id, p.name, p.created_at
                    UNION ALL
                    SELECT 2 AS module_order,
                           'maintenance' AS module_key,
                           'Mantenimientos' AS module_label,
                           mr.id AS entity_id,
                           mr.title AS entity_label,
                           CONCAT_WS(' | ', p.name, COALESCE(CAST(COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS TEXT), '')) AS context_detail,
                           COUNT(mri.id) AS image_count,
                           COALESCE(SUM(mri.size_bytes), 0) AS total_size_bytes,
                           MAX(mri.created_at) AS last_image_at,
                           COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS sort_date
                    FROM maintenance_records mr
                    JOIN properties p ON p.id = mr.property_id
                        AND p.organization_id = mr.organization_id
                        AND p.deleted_at IS NULL
                    LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                        AND mri.organization_id = mr.organization_id
                        AND mri.status = 'ACTIVE'
                    WHERE mr.organization_id = :organizationId
                      AND mr.deleted_at IS NULL
                    GROUP BY mr.id, mr.title, p.name, mr.performed_at, mr.scheduled_at, mr.created_at
                    UNION ALL
                    SELECT 3 AS module_order,
                           'inventory_items' AS module_key,
                           'Items de inventario' AS module_label,
                           ii.id AS entity_id,
                           ii.name AS entity_label,
                           CONCAT_WS(' | ', NULLIF(b.name, ''), NULLIF(ii.item_type, '')) AS context_detail,
                           COUNT(iii.id) AS image_count,
                           COALESCE(SUM(iii.size_bytes), 0) AS total_size_bytes,
                           MAX(iii.created_at) AS last_image_at,
                           ii.created_at AS sort_date
                    FROM inventory_items ii
                    LEFT JOIN brands b ON b.id = ii.brand_id
                        AND b.organization_id = ii.organization_id
                        AND b.deleted_at IS NULL
                    LEFT JOIN inventory_item_images iii ON iii.inventory_item_id = ii.id
                        AND iii.organization_id = ii.organization_id
                        AND iii.status = 'ACTIVE'
                    WHERE ii.organization_id = :organizationId
                      AND ii.deleted_at IS NULL
                    GROUP BY ii.id, ii.name, b.name, ii.item_type, ii.created_at
                    UNION ALL
                    SELECT 4 AS module_order,
                           'purchase_lists' AS module_key,
                           'Listas de compra' AS module_label,
                           pl.id AS entity_id,
                           CONCAT('Compra ', COALESCE(CAST(pl.purchase_date AS TEXT), 'sin fecha')) AS entity_label,
                           CONCAT_WS(' | ', COALESCE(p.name, 'Sin propiedad'), NULLIF(s.name, ''), NULLIF(pl.status, '')) AS context_detail,
                           COUNT(pui.id) AS image_count,
                           COALESCE(SUM(pui.size_bytes), 0) AS total_size_bytes,
                           MAX(pui.created_at) AS last_image_at,
                           pl.purchase_date AS sort_date
                    FROM purchase_lists pl
                    LEFT JOIN properties p ON p.id = pl.property_id
                        AND p.organization_id = pl.organization_id
                        AND p.deleted_at IS NULL
                    LEFT JOIN suppliers s ON s.id = pl.supplier_id
                        AND s.organization_id = pl.organization_id
                        AND s.deleted_at IS NULL
                    LEFT JOIN purchase_images pui ON pui.purchase_list_id = pl.id
                        AND pui.organization_id = pl.organization_id
                        AND pui.status = 'ACTIVE'
                    WHERE pl.organization_id = :organizationId
                      AND pl.deleted_at IS NULL
                    GROUP BY pl.id, pl.purchase_date, p.name, s.name, pl.status
                    UNION ALL
                    SELECT 5 AS module_order,
                           'reservations' AS module_key,
                           'Reservaciones' AS module_label,
                           r.id AS entity_id,
                           CONCAT('Reservación ', COALESCE(NULLIF(r.reservation_code, ''), CAST(r.check_in AS TEXT))) AS entity_label,
                           CONCAT_WS(' | ', p.name, CAST(r.check_in AS TEXT), CAST(r.check_out AS TEXT), NULLIF(r.status, '')) AS context_detail,
                           COUNT(ri.id) AS image_count,
                           COALESCE(SUM(ri.size_bytes), 0) AS total_size_bytes,
                           MAX(ri.created_at) AS last_image_at,
                           r.check_in AS sort_date
                    FROM reservations r
                    JOIN properties p ON p.id = r.property_id
                        AND p.organization_id = r.organization_id
                        AND p.deleted_at IS NULL
                    LEFT JOIN reservation_images ri ON ri.reservation_id = r.id
                        AND ri.organization_id = r.organization_id
                        AND ri.status = 'ACTIVE'
                    WHERE r.organization_id = :organizationId
                      AND r.deleted_at IS NULL
                    GROUP BY r.id, r.reservation_code, p.name, r.check_in, r.check_out, r.status
                ), filtered AS (
                    SELECT *
                    FROM candidates
                    %s
                ), ranked AS (
                    SELECT *, ROW_NUMBER() OVER (PARTITION BY module_key ORDER BY sort_date DESC NULLS LAST, entity_label ASC) AS module_rank
                    FROM filtered
                )
                SELECT module_key,
                       module_label,
                       entity_id,
                       entity_label,
                       context_detail,
                       image_count,
                       total_size_bytes,
                       last_image_at,
                       sort_date
                FROM ranked
                WHERE (:withoutImages = FALSE OR module_rank <= :limitPerModule)
                ORDER BY %s
                LIMIT :limit
                """.formatted(havingClause, ordering);

        return query(sql, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("withoutImages", withoutImages);
            q.setParameter("limitPerModule", limitPerModule);
            q.setParameter("limit", withoutImages ? limitPerModule * 5 : DEFAULT_LIMIT);
        }, "moduleKey", "moduleLabel", "entityId", "entityLabel", "contextDetail", "imageCount", "totalSizeBytes", "lastImageAt", "sortDate");
    }

    private void appendEntityImageCountRows(StringBuilder answer, List<Map<String, Object>> rows, boolean withoutImages) {
        String currentModule = null;
        for (Map<String, Object> row : rows) {
            String moduleLabel = blankToDash(value(row.get("moduleLabel")));
            if (!moduleLabel.equals(currentModule)) {
                answer.append(System.lineSeparator())
                        .append(System.lineSeparator())
                        .append("- ")
                        .append(moduleLabel)
                        .append(":");
                currentModule = moduleLabel;
            }

            answer.append(System.lineSeparator())
                    .append("  - ")
                    .append(blankToDash(value(row.get("entityLabel"))));
            String contextDetail = value(row.get("contextDetail"));
            if (!contextDetail.isBlank()) {
                answer.append(" | ").append(contextDetail);
            }
            if (!withoutImages) {
                answer.append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))))
                        .append(" | tamaño: ").append(formatBytes(toLong(row.get("totalSizeBytes"))));
            }
        }
    }

    private void appendGroupedFileMetadataRows(StringBuilder answer, List<Map<String, Object>> rows) {
        Map<String, List<Map<String, Object>>> groupedRows = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = value(row.get("displayName"))
                    + "\u0001" + value(row.get("sourceType"))
                    + "\u0001" + value(row.get("propertyName"));
            groupedRows.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(row);
        }

        for (List<Map<String, Object>> group : groupedRows.values()) {
            Map<String, Object> first = group.get(0);
            answer.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(first.get("displayName"))))
                    .append(" | origen: ")
                    .append(blankToDash(value(first.get("sourceType"))))
                    .append(" | propiedad: ")
                    .append(blankToDash(value(first.get("propertyName"))));

            for (Map<String, Object> row : group) {
                answer.append(System.lineSeparator())
                        .append("  - archivo: ")
                        .append(blankToDash(value(row.get("originalFilename"))))
                        .append(" | tipo: ")
                        .append(blankToDash(value(row.get("contentType"))))
                        .append(" | tamaño: ")
                        .append(formatBytes(toLong(row.get("sizeBytes"))));
            }
        }
    }

    private List<Map<String, Object>> fileMetadataRows(String search, String propertySearch, int limit) {
        return fileMetadataRows(search, propertySearch, limit, "created_at DESC");
    }

    private List<Map<String, Object>> fileMetadataRows(String search, String propertySearch, int limit, String orderBy) {
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
                    UNION ALL
                    SELECT 'INVENTORY_ITEM_IMAGE' AS source_type,
                           ii.name AS display_name,
                           iii.original_filename,
                           iii.content_type,
                           iii.size_bytes,
                           iii.status,
                           COALESCE(CONCAT_WS(' | ', NULLIF(b.name, ''), NULLIF(ii.item_type, '')), 'IMAGE') AS detail_status,
                           'Catálogos' AS property_name,
                           iii.created_at
                    FROM inventory_item_images iii
                    JOIN inventory_items ii ON ii.id = iii.inventory_item_id
                        AND ii.organization_id = iii.organization_id
                        AND ii.deleted_at IS NULL
                    LEFT JOIN brands b ON b.id = ii.brand_id
                        AND b.organization_id = ii.organization_id
                        AND b.deleted_at IS NULL
                    WHERE iii.organization_id = :organizationId
                      AND iii.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'PURCHASE_IMAGE' AS source_type,
                           CONCAT('Compra ', COALESCE(CAST(pl.purchase_date AS TEXT), 'sin fecha')) AS display_name,
                           pui.original_filename,
                           pui.content_type,
                           pui.size_bytes,
                           pui.status,
                           COALESCE(s.name, pl.status, 'IMAGE') AS detail_status,
                           COALESCE(p.name, 'Sin propiedad') AS property_name,
                           pui.created_at
                    FROM purchase_images pui
                    JOIN purchase_lists pl ON pl.id = pui.purchase_list_id
                        AND pl.organization_id = pui.organization_id
                        AND pl.deleted_at IS NULL
                    LEFT JOIN properties p ON p.id = pl.property_id
                        AND p.organization_id = pl.organization_id
                        AND p.deleted_at IS NULL
                    LEFT JOIN suppliers s ON s.id = pl.supplier_id
                        AND s.organization_id = pl.organization_id
                        AND s.deleted_at IS NULL
                    WHERE pui.organization_id = :organizationId
                      AND pui.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'RESERVATION_IMAGE' AS source_type,
                           CONCAT('Reservación ', COALESCE(NULLIF(r.reservation_code, ''), CAST(r.check_in AS TEXT))) AS display_name,
                           ri.original_filename,
                           ri.content_type,
                           ri.size_bytes,
                           ri.status,
                           COALESCE(platform.name, r.status, 'IMAGE') AS detail_status,
                           p.name AS property_name,
                           ri.created_at
                    FROM reservation_images ri
                    JOIN reservations r ON r.id = ri.reservation_id
                        AND r.organization_id = ri.organization_id
                        AND r.deleted_at IS NULL
                    JOIN properties p ON p.id = r.property_id
                        AND p.organization_id = r.organization_id
                        AND p.deleted_at IS NULL
                    LEFT JOIN platforms platform ON platform.id = r.platform_id
                        AND platform.organization_id = r.organization_id
                        AND platform.deleted_at IS NULL
                    WHERE ri.organization_id = :organizationId
                      AND ri.status = 'ACTIVE'
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
        sql.append(" ORDER BY ").append(orderBy).append(" LIMIT :limit");

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
