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
public class PropertyCatalogToolRepository extends AiReadOnlyToolSupport {

    public PropertyCatalogToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer searchProperties(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "propiedad", "propiedades", "alojamiento", "alojamientos", "casa", "casas", "bungalow", "bungalows"
        ));

        List<Map<String, Object>> rows = propertySearchRows(organizationId, search, null, DEFAULT_LIMIT);

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré propiedades registradas en tu organización."
                            : "No encontré propiedades que coincidan con “" + search + "”.",
                    "property.search",
                    "Properties",
                    "No matching properties found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Tienes estas propiedades registradas:"
                : "Encontré estas propiedades relacionadas con “" + search + "”:");
        appendPropertyList(answer, rows);

        return AiToolAnswer.of(
                answer.toString(),
                "property.search",
                "Properties",
                "%d properties found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer activeProperties() {
        return propertiesByStatus("ACTIVE", "property.getActiveProperties", "Active properties");
    }

    public AiToolAnswer inactiveProperties() {
        return propertiesByStatus("INACTIVE", "property.getInactiveProperties", "Inactive properties");
    }

    public AiToolAnswer propertySummary(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "resumen", "resume", "dame", "propiedad", "propiedades", "alojamiento", "alojamientos"
        ));

        List<Map<String, Object>> candidates = query("""
                SELECT p.id,
                       p.name,
                       p.status,
                       p.address,
                       p.description,
                       COUNT(DISTINCT pi.id) AS image_count,
                       COUNT(DISTINCT CASE WHEN r.status = 'ACTIVE' AND r.deleted_at IS NULL THEN r.id END) AS active_reservation_count,
                       COUNT(DISTINCT CASE WHEN mr.status = 'COMPLETED' AND mr.deleted_at IS NULL THEN mr.id END) AS completed_maintenance_count,
                       COUNT(DISTINCT CASE WHEN tl.status IN ('OPEN', 'IN_PROGRESS') AND tl.deleted_at IS NULL THEN tl.id END) AS open_task_list_count
                FROM properties p
                LEFT JOIN property_images pi ON pi.property_id = p.id
                                            AND pi.organization_id = p.organization_id
                                            AND pi.deleted_at IS NULL
                                            AND pi.status = 'ACTIVE'
                LEFT JOIN reservations r ON r.property_id = p.id
                                         AND r.organization_id = p.organization_id
                LEFT JOIN maintenance_records mr ON mr.property_id = p.id
                                                AND mr.organization_id = p.organization_id
                LEFT JOIN task_lists tl ON tl.property_id = p.id
                                       AND tl.organization_id = p.organization_id
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name, p.status, p.address, p.description
                ORDER BY p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", 50);
                }, "id", "name", "status", "address", "description", "imageCount", "activeReservationCount",
                "completedMaintenanceCount", "openTaskListCount");

        Map<String, Object> row = bestPropertyMatch(candidates, search);
        if (row == null) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré propiedades para resumir."
                            : "No encontré una propiedad que coincida con “" + search + "”.",
                    "property.getSummary",
                    "Property summary",
                    "No matching property summary found.",
                    List.of()
            );
        }

        List<Map<String, Object>> rows = List.of(row);
        String answer = """
                Este es el resumen de %s:
                - Estado: %s
                - Dirección: %s
                - Imágenes activas: %s
                - Reservaciones activas asociadas: %s
                - Mantenimientos completados: %s
                - Listas de tareas abiertas/en progreso: %s
                """.formatted(
                blankToDash(value(row.get("name"))),
                blankToDash(value(row.get("status"))),
                blankToDash(value(row.get("address"))),
                blankToDash(value(row.get("imageCount"))),
                blankToDash(value(row.get("activeReservationCount"))),
                blankToDash(value(row.get("completedMaintenanceCount"))),
                blankToDash(value(row.get("openTaskListCount")))
        ).trim();

        String description = value(row.get("description"));
        if (!description.isBlank()) {
            answer += "\n- Descripción: " + description;
        }

        return AiToolAnswer.of(
                answer,
                "property.getSummary",
                "Property summary",
                "A single property summary was consulted.",
                rows
        );
    }

    public AiToolAnswer propertyOperationalOverview() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate nextSevenDays = today.plusDays(7);
        List<Map<String, Object>> rows = query("""
                SELECT p.id,
                       p.name,
                       p.status,
                       COUNT(DISTINCT CASE WHEN r.status = 'ACTIVE'
                                             AND r.deleted_at IS NULL
                                             AND r.check_in BETWEEN :today AND :nextSevenDays THEN r.id END) AS upcoming_reservations_7_days,
                       COUNT(DISTINCT CASE WHEN sm.status = 'ACTIVE'
                                             AND sm.deleted_at IS NULL
                                             AND sm.next_due_date < :today THEN sm.id END) AS overdue_scheduled_maintenance,
                       COUNT(DISTINCT CASE WHEN tl.status IN ('OPEN', 'IN_PROGRESS')
                                             AND tl.deleted_at IS NULL THEN tl.id END) AS open_task_lists,
                       COUNT(DISTINCT CASE WHEN mr.status = 'COMPLETED'
                                             AND mr.deleted_at IS NULL THEN mr.id END) AS completed_maintenance_count
                FROM properties p
                LEFT JOIN reservations r ON r.property_id = p.id AND r.organization_id = p.organization_id
                LEFT JOIN scheduled_maintenance sm ON sm.property_id = p.id AND sm.organization_id = p.organization_id
                LEFT JOIN task_lists tl ON tl.property_id = p.id AND tl.organization_id = p.organization_id
                LEFT JOIN maintenance_records mr ON mr.property_id = p.id AND mr.organization_id = p.organization_id
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name, p.status
                ORDER BY overdue_scheduled_maintenance DESC,
                         upcoming_reservations_7_days DESC,
                         open_task_lists DESC,
                         p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("nextSevenDays", Date.valueOf(nextSevenDays));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "status", "upcomingReservations7Days", "overdueScheduledMaintenance",
                "openTaskLists", "completedMaintenanceCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré propiedades para calcular el panorama operativo.",
                    "property.getOperationalOverview",
                    "Property operational overview",
                    "No properties found for operational overview.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Este es el panorama operativo por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | reservas próximos 7 días: ").append(blankToDash(value(row.get("upcomingReservations7Days"))))
                    .append(" | mantenimientos vencidos: ").append(blankToDash(value(row.get("overdueScheduledMaintenance"))))
                    .append(" | tareas abiertas: ").append(blankToDash(value(row.get("openTaskLists"))))
                    .append(" | mantenimientos completados: ").append(blankToDash(value(row.get("completedMaintenanceCount"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "property.getOperationalOverview",
                "Property operational overview",
                "%d property operational rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer propertyImagesSummary(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        boolean onlyWithoutImages = containsAny(normalize(userQuestion), "sin imagen", "sin imagenes", "no tienen imagen", "no tiene imagen");

        List<Map<String, Object>> rows = query("""
                SELECT p.id,
                       p.name,
                       p.status,
                       COUNT(pi.id) AS image_count,
                       COALESCE(SUM(CASE WHEN pi.is_cover = TRUE THEN 1 ELSE 0 END), 0) AS cover_image_count,
                       MAX(pi.created_at) AS last_image_created_at
                FROM properties p
                LEFT JOIN property_images pi ON pi.property_id = p.id
                                            AND pi.organization_id = p.organization_id
                                            AND pi.deleted_at IS NULL
                                            AND pi.status = 'ACTIVE'
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name, p.status
                HAVING CAST(:onlyWithoutImages AS BOOLEAN) = FALSE OR COUNT(pi.id) = 0
                ORDER BY image_count ASC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("onlyWithoutImages", onlyWithoutImages);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "status", "imageCount", "coverImageCount", "lastImageCreatedAt");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    onlyWithoutImages
                            ? "Todas las propiedades consultadas tienen al menos una imagen activa."
                            : "No encontré metadata de imágenes para tus propiedades.",
                    "property.getImagesSummary",
                    "Property images summary",
                    "No property image rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(onlyWithoutImages
                ? "Estas propiedades no tienen imágenes activas:"
                : "Así están las imágenes por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))))
                    .append(" | portada: ").append(blankToDash(value(row.get("coverImageCount"))))
                    .append(" | última imagen: ").append(blankToDash(value(row.get("lastImageCreatedAt"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "property.getImagesSummary",
                "Property images summary",
                "%d property image summary rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer maintenanceCategories() {
        return baseCatalog("maintenance_categories", "catalog.maintenanceCategories", "Maintenance categories", "categorías de mantenimiento");
    }

    public AiToolAnswer maintenanceTypes() {
        return baseCatalog("maintenance_types", "catalog.maintenanceTypes", "Maintenance types", "tipos de mantenimiento");
    }

    public AiToolAnswer maintenanceCatalogOverview() {
        AiToolAnswer categories = maintenanceCategories();
        AiToolAnswer types = maintenanceTypes();
        AiToolAnswer itemTypes = inventoryItemTypes();

        String answer = """
                Para mantenimiento puedes apoyarte principalmente en estos catálogos:

                1. Categorías de mantenimiento: agrupan el área general del trabajo, por ejemplo agua, bombas, cisterna o filtros.
                %s

                2. Tipos de mantenimiento: describen la acción concreta, por ejemplo limpieza, revisión, reparación o cambio.
                %s

                3. Tipos de items de inventario: te ayudan a clasificar supplies/repuestos que luego pueden usarse en mantenimientos.
                %s
                """.formatted(
                indentCatalogAnswer(categories.answer()),
                indentCatalogAnswer(types.answer()),
                indentCatalogAnswer(itemTypes.answer())
        ).trim();

        List<AiToolEvidenceResponse> evidence = new ArrayList<>();
        evidence.addAll(categories.evidence());
        evidence.addAll(types.evidence());
        evidence.addAll(itemTypes.evidence());
        evidence.add(new AiToolEvidenceResponse(
                "catalog.maintenanceOverview",
                "Maintenance catalog overview",
                "Maintenance categories, maintenance types and inventory item types were consulted together.",
                List.of()
        ));
        return new AiToolAnswer(answer, true, evidence);
    }

    public AiToolAnswer reservationPlatforms() {
        return baseCatalog("platforms", "catalog.reservationPlatforms", "Reservation platforms", "plataformas de reservación");
    }

    public AiToolAnswer taskCategories() {
        return baseCatalog("task_templates", "catalog.taskCategories", "Task templates", "plantillas/categorías de tareas");
    }

    public AiToolAnswer purchaseCategories() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT ii.item_type,
                       COUNT(ii.id) AS item_count,
                       COALESCE(STRING_AGG(ii.name, ', ' ORDER BY ii.name), '') AS sample_items
                FROM inventory_items ii
                WHERE ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
                  AND ii.status = 'ACTIVE'
                  AND ii.available_for_purchases = TRUE
                GROUP BY ii.item_type
                ORDER BY ii.item_type ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "itemType", "itemCount", "sampleItems");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré items activos disponibles para compras. En el schema actual no hay una tabla separada de categorías de compras; TAMIAS usa los tipos de inventory items para clasificar supplies/materiales.",
                    "catalog.purchaseCategories",
                    "Purchase categories",
                    "No purchase-ready inventory item groups found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Para compras, TAMIAS está usando items de inventario disponibles para compras, agrupados por tipo:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemType"))))
                    .append(" | items: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | ejemplos: ").append(blankToDash(value(row.get("sampleItems"))));
        }
        answer.append("\n\nNota: no encontré una tabla dedicada llamada purchase_categories en el schema actual; esta respuesta usa metadata real de inventory_items.");

        return AiToolAnswer.of(
                answer.toString(),
                "catalog.purchaseCategories",
                "Purchase categories",
                "%d purchase category groups found from inventory item types.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryItemTypes() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(Map.of("code", "MATERIAL", "description", "Material o repuesto"));
        rows.add(Map.of("code", "SUPPLY", "description", "Supply operativo"));
        rows.add(Map.of("code", "AMENITY", "description", "Amenity para huéspedes"));
        rows.add(Map.of("code", "CLEANING_SUPPLY", "description", "Producto de limpieza"));
        rows.add(Map.of("code", "TOOL", "description", "Herramienta"));
        rows.add(Map.of("code", "OTHER", "description", "Otro tipo"));
        String answer = "Estos son los tipos de inventory item configurados en el código de TAMIAS:\n"
                + rows.stream()
                .map(row -> "- " + row.get("code") + " — " + row.get("description"))
                .collect(Collectors.joining(System.lineSeparator()));
        return AiToolAnswer.of(
                answer,
                "catalog.inventoryItemTypes",
                "Inventory item types",
                "Inventory item enum values were listed.",
                rows
        );
    }

    public AiToolAnswer catalogSearch(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "catalogo", "catalogos", "mantenimiento", "mantenimientos", "compra", "compras", "tarea", "tareas",
                "reservacion", "reservaciones", "tipo", "tipos", "categoria", "categorias", "configurado", "configurados"
        ));

        List<Map<String, Object>> rows = query("""
                SELECT * FROM (
                    SELECT 'maintenanceCategory' AS catalog_type, id, name, description, status FROM maintenance_categories
                    WHERE organization_id = :organizationId AND deleted_at IS NULL
                    UNION ALL
                    SELECT 'maintenanceType' AS catalog_type, id, name, description, status FROM maintenance_types
                    WHERE organization_id = :organizationId AND deleted_at IS NULL
                    UNION ALL
                    SELECT 'reservationPlatform' AS catalog_type, id, name, description, status FROM platforms
                    WHERE organization_id = :organizationId AND deleted_at IS NULL
                    UNION ALL
                    SELECT 'taskTemplate' AS catalog_type, id, name, description, status FROM task_templates
                    WHERE organization_id = :organizationId AND deleted_at IS NULL
                    UNION ALL
                    SELECT 'inventoryItem' AS catalog_type, id, name, description, status FROM inventory_items
                    WHERE organization_id = :organizationId AND deleted_at IS NULL
                ) catalog
                WHERE CAST(:search AS TEXT) IS NULL
                   OR LOWER(catalog.name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                   OR LOWER(COALESCE(catalog.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                   OR LOWER(catalog.catalog_type) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                ORDER BY catalog_type ASC, name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "catalogType", "id", "name", "description", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré catálogos configurados en tu organización."
                            : "No encontré catálogos que coincidan con “" + search + "”.",
                    "catalog.search",
                    "Catalog search",
                    "No matching catalog rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Encontré estos catálogos configurados:"
                : "Encontré estos catálogos relacionados con “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- [").append(blankToDash(value(row.get("catalogType")))).append("] ")
                    .append(blankToDash(value(row.get("name"))))
                    .append(" — estado: ").append(blankToDash(value(row.get("status"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "catalog.search",
                "Catalog search",
                "%d catalog rows found.".formatted(rows.size()),
                rows
        );
    }
}
