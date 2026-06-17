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
                       p.description
                FROM properties p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                ORDER BY p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", 50);
                }, "id", "name", "status", "address", "description");

        Map<String, Object> property = bestPropertyMatch(candidates, search);
        if (property == null) {
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

        UUID propertyId = UUID.fromString(value(property.get("id")));
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> reservations = propertyUpcomingReservationRows(organizationId, propertyId, today);
        List<Map<String, Object>> scheduledMaintenance = propertyUpcomingScheduledMaintenanceRows(organizationId, propertyId, today);
        List<Map<String, Object>> pendingMaintenance = propertyPendingMaintenanceRows(organizationId, propertyId);
        List<Map<String, Object>> pendingTaskItems = propertyPendingTaskItemRows(organizationId, propertyId);

        StringBuilder answer = new StringBuilder("Resumen operativo de ")
                .append(blankToDash(value(property.get("name"))))
                .append(":");

        appendPropertyUpcomingReservations(answer, reservations);
        appendPropertyScheduledMaintenance(answer, scheduledMaintenance);
        appendPropertyPendingMaintenance(answer, pendingMaintenance);
        appendPropertyPendingTaskItems(answer, pendingTaskItems);

        List<Map<String, Object>> evidenceRows = new ArrayList<>();
        evidenceRows.add(property);
        evidenceRows.addAll(reservations);
        evidenceRows.addAll(scheduledMaintenance);
        evidenceRows.addAll(pendingMaintenance);
        evidenceRows.addAll(pendingTaskItems);

        return AiToolAnswer.of(
                answer.toString(),
                "property.getSummary",
                "Property summary",
                "Property operational summary was generated from reservations, scheduled maintenance, maintenance records and task items.",
                evidenceRows
        );
    }

    private List<Map<String, Object>> propertyUpcomingReservationRows(UUID organizationId, UUID propertyId, LocalDate today) {
        return query("""
                SELECT r.id,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       (r.check_out - r.check_in) AS nights,
                       COALESCE(
                           MAX(CASE WHEN rg.is_primary = TRUE THEN g.full_name ELSE NULL END),
                           MIN(g.full_name),
                           ''
                       ) AS primary_guest
                FROM reservations r
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.property_id = :propertyId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in >= :today
                GROUP BY r.id, r.reservation_code, r.check_in, r.check_out
                ORDER BY r.check_in ASC, r.created_at ASC
                LIMIT 5
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("propertyId", propertyId);
                    q.setParameter("today", Date.valueOf(today));
                }, "id", "reservationCode", "checkIn", "checkOut", "nights", "primaryGuest");
    }

    private List<Map<String, Object>> propertyUpcomingScheduledMaintenanceRows(UUID organizationId, UUID propertyId, LocalDate today) {
        return query("""
                SELECT sm.id,
                       sm.title,
                       COALESCE(mp.name, '') AS person_name,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
                       sm.start_date,
                       sm.end_date,
                       sm.next_due_date,
                       sm.status
                FROM scheduled_maintenance sm
                LEFT JOIN maintenance_people mp ON mp.id = sm.maintenance_person_id AND mp.organization_id = sm.organization_id
                LEFT JOIN maintenance_categories mc ON mc.id = sm.maintenance_category_id AND mc.organization_id = sm.organization_id
                LEFT JOIN maintenance_types mt ON mt.id = sm.maintenance_type_id AND mt.organization_id = sm.organization_id
                WHERE sm.organization_id = :organizationId
                  AND sm.property_id = :propertyId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND (sm.end_date IS NULL OR sm.end_date >= :today)
                ORDER BY sm.start_date ASC, sm.next_due_date ASC, sm.title ASC
                LIMIT 5
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("propertyId", propertyId);
                    q.setParameter("today", Date.valueOf(today));
                }, "id", "title", "personName", "categoryName", "typeName", "startDate", "endDate", "nextDueDate", "status");
    }

    private List<Map<String, Object>> propertyPendingMaintenanceRows(UUID organizationId, UUID propertyId) {
        return query("""
                SELECT mr.id,
                       mr.title,
                       COALESCE(mp.name, '') AS person_name,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
                       mr.scheduled_at,
                       mr.status
                FROM maintenance_records mr
                LEFT JOIN maintenance_people mp ON mp.id = mr.maintenance_person_id AND mp.organization_id = mr.organization_id
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id AND mc.organization_id = mr.organization_id
                LEFT JOIN maintenance_types mt ON mt.id = mr.maintenance_type_id AND mt.organization_id = mr.organization_id
                WHERE mr.organization_id = :organizationId
                  AND mr.property_id = :propertyId
                  AND mr.deleted_at IS NULL
                  AND mr.status IN ('PENDING', 'IN_PROGRESS')
                ORDER BY mr.scheduled_at ASC NULLS LAST, mr.created_at DESC
                LIMIT 5
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("propertyId", propertyId);
                }, "id", "title", "personName", "categoryName", "typeName", "scheduledAt", "status");
    }

    private List<Map<String, Object>> propertyPendingTaskItemRows(UUID organizationId, UUID propertyId) {
        return query("""
                SELECT ti.id,
                       ti.task_name,
                       COALESCE(ti.responsible_person, '') AS responsible_person,
                       tl.title AS task_list_title,
                       tl.due_date
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                WHERE ti.organization_id = :organizationId
                  AND tl.property_id = :propertyId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND ti.completed = FALSE
                ORDER BY tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC
                LIMIT 10
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("propertyId", propertyId);
                }, "id", "taskName", "responsiblePerson", "taskListTitle", "dueDate");
    }

    private void appendPropertyUpcomingReservations(StringBuilder answer, List<Map<String, Object>> rows) {
        answer.append(System.lineSeparator()).append(System.lineSeparator()).append("Próximas reservaciones:");
        if (rows.isEmpty()) {
            answer.append(System.lineSeparator()).append("- No encontré próximas reservaciones activas para esta propiedad.");
            return;
        }
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))))
                    .append(" | huésped principal: ").append(blankToDash(value(row.get("primaryGuest"))))
                    .append(" | noches: ").append(blankToDash(value(row.get("nights"))));
            String code = value(row.get("reservationCode"));
            if (!code.isBlank()) {
                answer.append(" | código: ").append(code);
            }
        }
    }

    private void appendPropertyScheduledMaintenance(StringBuilder answer, List<Map<String, Object>> rows) {
        answer.append(System.lineSeparator()).append(System.lineSeparator()).append("Mantenimientos programados próximos:");
        if (rows.isEmpty()) {
            answer.append(System.lineSeparator()).append("- No encontré mantenimientos programados activos para esta propiedad.");
            return;
        }
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | responsable: ").append(blankToDash(value(row.get("personName"))))
                    .append(" | categoría/tipo: ").append(blankToDash(value(row.get("categoryName"))));
            if (!value(row.get("typeName")).isBlank()) {
                answer.append(" / ").append(value(row.get("typeName")));
            }
            answer.append(" | inicio: ").append(blankToDash(value(row.get("startDate"))))
                    .append(" | finalización: ").append(blankToDash(value(row.get("endDate"))))
                    .append(" | próximo vencimiento: ").append(blankToDash(value(row.get("nextDueDate"))));
        }
    }

    private void appendPropertyPendingMaintenance(StringBuilder answer, List<Map<String, Object>> rows) {
        answer.append(System.lineSeparator()).append(System.lineSeparator()).append("Mantenimientos pendientes:");
        if (rows.isEmpty()) {
            answer.append(System.lineSeparator()).append("- No encontré mantenimientos pendientes para esta propiedad.");
            return;
        }
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | responsable: ").append(blankToDash(value(row.get("personName"))))
                    .append(" | categoría/tipo: ").append(blankToDash(value(row.get("categoryName"))));
            if (!value(row.get("typeName")).isBlank()) {
                answer.append(" / ").append(value(row.get("typeName")));
            }
            answer.append(" | programado para: ").append(blankToDash(value(row.get("scheduledAt"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
        }
    }

    private void appendPropertyPendingTaskItems(StringBuilder answer, List<Map<String, Object>> rows) {
        answer.append(System.lineSeparator()).append(System.lineSeparator()).append("Tareas específicas pendientes:");
        if (rows.isEmpty()) {
            answer.append(System.lineSeparator()).append("- No encontré tareas específicas pendientes para esta propiedad.");
            return;
        }
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("taskName"))))
                    .append(" | responsable: ").append(blankToDash(value(row.get("responsiblePerson"))))
                    .append(" | lista: ").append(blankToDash(value(row.get("taskListTitle"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("dueDate"))));
        }
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
        AiToolAnswer maintenanceItems = maintenanceInventoryItemsSummary();

        String answer = """
                Para mantenimiento puedes apoyarte principalmente en estos catálogos:

                1. Categorías de mantenimiento: agrupan el área general del trabajo, por ejemplo agua, bombas, cisterna o filtros.
                %s

                2. Tipos de mantenimiento: describen la acción concreta, por ejemplo limpieza, revisión, reparación o cambio.
                %s

                3. Items de inventario disponibles: %s
                """.formatted(
                indentCatalogAnswer(categories.answer()),
                indentCatalogAnswer(types.answer()),
                maintenanceItems.answer()
        ).trim();

        List<AiToolEvidenceResponse> evidence = new ArrayList<>();
        evidence.addAll(categories.evidence());
        evidence.addAll(types.evidence());
        evidence.addAll(maintenanceItems.evidence());
        evidence.add(new AiToolEvidenceResponse(
                "catalog.maintenanceOverview",
                "Maintenance catalog overview",
                "Maintenance categories, maintenance types and maintenance-ready inventory items were consulted together.",
                List.of()
        ));
        return new AiToolAnswer(answer, true, evidence);
    }

    private AiToolAnswer maintenanceInventoryItemsSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) AS item_count
                FROM inventory_items ii
                WHERE ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
                  AND ii.status = 'ACTIVE'
                  AND ii.available_for_maintenance = TRUE
                """, q -> q.setParameter("organizationId", organizationId), "itemCount");
        String count = rows.isEmpty() ? "0" : blankToDash(value(rows.get(0).get("itemCount")));
        return AiToolAnswer.of(
                count + " items disponibles para su uso en mantenimiento.",
                "catalog.maintenanceInventoryItems",
                "Maintenance inventory items",
                "Maintenance-ready inventory items were counted.",
                rows
        );
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
