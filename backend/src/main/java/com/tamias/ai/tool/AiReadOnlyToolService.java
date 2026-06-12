package com.tamias.ai.tool;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.Normalizer;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiReadOnlyToolService {

    private static final int DEFAULT_LIMIT = 10;

    private static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "al", "algo", "actual", "actuales", "actualmente", "ahi", "aqui",
            "cargado", "cargados", "con", "cual", "cuales", "cuando", "cuanto", "cuantos",
            "da", "dame", "de", "del", "dice", "e", "el", "en", "estado", "estan", "esta",
            "este", "estos", "fue", "hay", "indexado", "indexados", "la", "las", "le",
            "lista", "listar", "lo", "los", "me", "mi", "mis", "muestra", "nombre", "o",
            "para", "por", "procesado", "procesados", "que", "quiero", "reciente", "registrada",
            "registradas", "registrado", "registrados", "son", "subido", "subidos", "tengo",
            "tienes", "tipo", "tu", "un", "una", "ver", "vez", "y"
    );

    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;

    public AiReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
    }

    public AiToolAnswer capabilities() {
        String answer = """
                Soy el asistente IA de TAMIAS. Te ayudo a consultar información operativa de tus alojamientos sin modificar datos.

                Puedo apoyarte con propiedades, catálogos, reservaciones próximas, mantenimientos, compras, tareas, documentos y estado del índice RAG. También puedo combinar varias consultas para darte una visión operativa más útil.

                Por seguridad, en esta fase sigo siendo read-only: no creo, edito, elimino registros ni envío notificaciones automáticamente.
                """.trim();
        return AiToolAnswer.of(
                answer,
                "assistant.capabilities",
                "Assistant capabilities",
                "Static TAMIAS assistant capabilities response.",
                List.of()
        );
    }

    public AiToolAnswer currentUserProfile(String userQuestion) {
        UUID userId = currentUserService.getCurrentUserId();
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT u.first_name, u.last_name, u.email, u.status, u.password_change_required,
                       r.code AS role_code, o.name AS organization_name
                FROM users u
                JOIN user_organizations uo ON uo.user_id = u.id
                JOIN organizations o ON o.id = uo.organization_id
                JOIN roles r ON r.id = uo.role_id
                WHERE u.id = :userId
                  AND uo.organization_id = :organizationId
                  AND u.deleted_at IS NULL
                  AND o.deleted_at IS NULL
                LIMIT 1
                """, q -> {
                    q.setParameter("userId", userId);
                    q.setParameter("organizationId", organizationId);
                }, "firstName", "lastName", "email", "status", "passwordChangeRequired", "role", "organizationName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré información del usuario autenticado en TAMIAS.",
                    "user.currentProfile",
                    "Current user profile",
                    "No profile data found.",
                    List.of()
            );
        }

        Map<String, Object> profile = rows.get(0);
        String fullName = joinName(profile.get("firstName"), profile.get("lastName"));
        String email = value(profile.get("email"));
        String role = value(profile.get("role"));
        String organizationName = value(profile.get("organizationName"));
        String normalizedQuestion = normalize(userQuestion);

        String answer;
        if (containsAny(normalizedQuestion, "telefono", "numero de telefono", "celular")) {
            answer = "No veo un número de teléfono registrado en tu perfil de TAMIAS.";
        } else if (containsAny(normalizedQuestion, "correo", "email")) {
            answer = "El correo actual que estás usando es " + blankToDash(email) + ".";
        } else if (containsAny(normalizedQuestion, "usuario")) {
            answer = "Estás usando el usuario " + blankToDash(email) + ".";
        } else if (containsAny(normalizedQuestion, "rol")) {
            answer = "Tu rol actual en TAMIAS es " + blankToDash(role) + ".";
        } else if (containsAny(normalizedQuestion, "organizacion")) {
            answer = "Estás trabajando en la organización " + blankToDash(organizationName) + ".";
        } else if (containsAny(normalizedQuestion, "nombre", "llamo")) {
            answer = "Te llamas " + blankToDash(fullName) + ".";
        } else {
            answer = "Tienes sesión activa como " + blankToDash(fullName) + " (" + blankToDash(email)
                    + "), con rol " + blankToDash(role) + " dentro de " + blankToDash(organizationName) + ".";
        }

        return AiToolAnswer.of(
                answer,
                "user.currentProfile",
                "Current user profile",
                "Current authenticated user profile was consulted.",
                rows
        );
    }

    public AiToolAnswer currentOrganizationSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT o.name, o.description, o.status, COUNT(DISTINCT uo.user_id) AS user_count
                FROM organizations o
                LEFT JOIN user_organizations uo ON uo.organization_id = o.id AND uo.status = 'ACTIVE'
                WHERE o.id = :organizationId
                  AND o.deleted_at IS NULL
                GROUP BY o.id, o.name, o.description, o.status
                LIMIT 1
                """, q -> q.setParameter("organizationId", organizationId),
                "name", "description", "status", "userCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré la organización actual asociada a tu usuario.",
                    "organization.currentSummary",
                    "Current organization",
                    "No organization found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = "Estás trabajando en " + blankToDash(value(row.get("name"))) + ".\n"
                + "Estado: " + blankToDash(value(row.get("status"))) + ".\n"
                + "Usuarios activos asociados: " + blankToDash(value(row.get("userCount"))) + ".";
        return AiToolAnswer.of(
                answer,
                "organization.currentSummary",
                "Current organization",
                "Current organization summary was consulted.",
                rows
        );
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

        List<Map<String, Object>> rows = query("""
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
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                GROUP BY p.id, p.name, p.status, p.address, p.description
                ORDER BY CASE WHEN CAST(:search AS TEXT) IS NOT NULL AND LOWER(p.name) = LOWER(CAST(:search AS TEXT)) THEN 0 ELSE 1 END,
                         p.name ASC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "name", "status", "address", "description", "imageCount", "activeReservationCount",
                "completedMaintenanceCount", "openTaskListCount");

        if (rows.isEmpty()) {
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

        Map<String, Object> row = rows.get(0);
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

    public AiToolAnswer operationalSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate nextSevenDays = today.plusDays(7);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeProperties", scalar("""
                SELECT COUNT(*)
                FROM properties
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                """, q -> q.setParameter("organizationId", organizationId)));
        summary.put("upcomingReservations7Days", scalar("""
                SELECT COUNT(*)
                FROM reservations
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND check_in BETWEEN :today AND :nextSevenDays
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("nextSevenDays", Date.valueOf(nextSevenDays));
                }));
        summary.put("overdueScheduledMaintenance", scalar("""
                SELECT COUNT(*)
                FROM scheduled_maintenance
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND next_due_date < :today
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }));
        summary.put("openTaskLists", scalar("""
                SELECT COUNT(*)
                FROM task_lists
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status IN ('OPEN', 'IN_PROGRESS')
                """, q -> q.setParameter("organizationId", organizationId)));
        summary.put("documentsNotIndexed", scalar("""
                SELECT COUNT(*)
                FROM documents d
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM document_chunks dc
                      WHERE dc.document_id = d.id
                        AND dc.organization_id = d.organization_id
                        AND dc.vector_store_id IS NOT NULL
                  )
                """, q -> q.setParameter("organizationId", organizationId)));

        List<Map<String, Object>> rows = List.of(summary);
        String answer = """
                Así va tu operación hoy:
                - Propiedades activas: %s
                - Reservas con check-in en los próximos 7 días: %s
                - Mantenimientos programados vencidos: %s
                - Listas de tareas abiertas/en progreso: %s
                - Documentos sin evidencia de indexación IA: %s
                """.formatted(
                summary.get("activeProperties"),
                summary.get("upcomingReservations7Days"),
                summary.get("overdueScheduledMaintenance"),
                summary.get("openTaskLists"),
                summary.get("documentsNotIndexed")
        ).trim();
        return AiToolAnswer.of(
                answer,
                "dashboard.operationalSummary",
                "Operational summary",
                "Operational counters were calculated.",
                rows
        );
    }

    public AiToolAnswer upcomingReservations() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(14);
        List<Map<String, Object>> rows = query("""
                SELECT r.id,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.reservation_value,
                       r.status,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id
                                                AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id
                                  AND g.organization_id = r.organization_id
                                  AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in BETWEEN :today AND :until
                GROUP BY r.id, p.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status
                ORDER BY r.check_in ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("until", Date.valueOf(until));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "reservationCode", "checkIn", "checkOut", "reservationValue", "status", "guests");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré reservaciones activas con check-in en los próximos 14 días.",
                    "reservation.upcoming",
                    "Upcoming reservations",
                    "No upcoming reservations found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas son tus próximas reservaciones activas:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))));
            String reservationCode = value(row.get("reservationCode"));
            if (!reservationCode.isBlank()) {
                answer.append(" | Código: ").append(reservationCode);
            }
            String guests = value(row.get("guests"));
            if (!guests.isBlank()) {
                answer.append(" | Huéspedes: ").append(guests);
            }
        }

        return AiToolAnswer.of(
                answer.toString(),
                "reservation.upcoming",
                "Upcoming reservations",
                "%d upcoming reservations found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer lastPerformedMaintenance(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "ultimo", "ultima", "mantenimiento", "mantenimientos", "realizado", "realizados",
                "completado", "completados", "reciente", "recientes"
        ));
        List<Map<String, Object>> rows = query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.description,
                       mc.name AS category_name,
                       mt.name AS type_name,
                       mr.performed_at,
                       mr.scheduled_at,
                       mr.cost,
                       mr.status
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = mr.maintenance_type_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.status = 'COMPLETED'
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(mr.title) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mr.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mc.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mt.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "propertyName", "title", "description", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré mantenimientos completados en tu organización."
                            : "No encontré mantenimientos completados relacionados con “" + search + "”.",
                    "maintenance.lastPerformed",
                    "Last performed maintenance",
                    "No matching maintenance record found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = """
                El último mantenimiento completado que encontré fue:
                - Propiedad: %s
                - Título: %s
                - Categoría: %s
                - Tipo: %s
                - Fecha realizada: %s
                - Estado: %s
                - Costo: %s
                """.formatted(
                blankToDash(value(row.get("propertyName"))),
                blankToDash(value(row.get("title"))),
                blankToDash(value(row.get("categoryName"))),
                blankToDash(value(row.get("typeName"))),
                blankToDash(value(row.get("performedAt"))),
                blankToDash(value(row.get("status"))),
                formatMoney(row.get("cost"))
        ).trim();
        return AiToolAnswer.of(
                answer,
                "maintenance.lastPerformed",
                "Last performed maintenance",
                "Most recent matching completed maintenance record found.",
                rows
        );
    }

    public AiToolAnswer overdueScheduledMaintenance() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                SELECT sm.id,
                       p.name AS property_name,
                       sm.title,
                       mc.name AS category_name,
                       mt.name AS type_name,
                       sm.frequency,
                       sm.next_due_date,
                       (CAST(:today AS DATE) - sm.next_due_date) AS days_overdue,
                       sm.status
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = sm.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = sm.maintenance_type_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND sm.next_due_date < :today
                ORDER BY sm.next_due_date ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "title", "categoryName", "typeName", "frequency", "nextDueDate", "daysOverdue", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré mantenimientos programados vencidos.",
                    "scheduledMaintenance.overdue",
                    "Overdue scheduled maintenance",
                    "No overdue scheduled maintenance found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos mantenimientos programados ya están vencidos:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("title"))))
                    .append(" | vencía el ").append(blankToDash(value(row.get("nextDueDate"))))
                    .append(" | días vencido: ").append(blankToDash(value(row.get("daysOverdue"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "scheduledMaintenance.overdue",
                "Overdue scheduled maintenance",
                "%d overdue scheduled maintenance records found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer lastPurchasedItem(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "cuando", "compre", "compraste", "compro", "compra", "compras", "comprado", "comprada",
                "ultima", "ultimo", "vez", "item", "producto"
        ));
        List<Map<String, Object>> rows = query("""
                SELECT pi.id,
                       pi.item_name_snapshot,
                       pi.quantity,
                       pi.unit,
                       pi.estimated_price,
                       pi.purchased,
                       pl.purchase_date,
                       pl.status AS purchase_list_status,
                       p.name AS property_name,
                       s.name AS supplier_name
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                LEFT JOIN properties p ON p.id = pl.property_id
                LEFT JOIN suppliers s ON s.id = pl.supplier_id
                WHERE pi.organization_id = :organizationId
                  AND pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                  AND pi.purchased = TRUE
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                ORDER BY CASE
                         WHEN CAST(:search AS TEXT) IS NOT NULL
                              AND LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)) = CAST(:search AS TEXT) THEN 0
                         WHEN CAST(:search AS TEXT) IS NOT NULL
                              AND LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)) LIKE CONCAT('%', CAST(:search AS TEXT), '%') THEN 1
                         ELSE 2
                         END,
                         pl.purchase_date DESC,
                         pi.created_at DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "itemName", "quantity", "unit", "estimatedPrice", "purchased", "purchaseDate", "purchaseListStatus", "propertyName", "supplierName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré items marcados como comprados."
                            : "No encontré una compra marcada como comprada para “" + search + "”.\nRevisé los items comprados usando coincidencia por palabras, no solo por frase exacta.",
                    "purchaseItem.lastPurchased",
                    "Last purchased item",
                    "No matching purchased item found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String itemName = blankToDash(value(row.get("itemName")));
        String purchaseDate = blankToDash(value(row.get("purchaseDate")));
        String answer = "La última vez que encontré comprado “" + itemName + "” fue el " + purchaseDate + ".\n"
                + "Cantidad: " + blankToDash(value(row.get("quantity"))) + " " + blankToDash(value(row.get("unit"))) + ".\n"
                + "Precio estimado: " + formatMoney(row.get("estimatedPrice")) + ".\n"
                + "Propiedad: " + blankToDash(value(row.get("propertyName"))) + ".\n"
                + "Proveedor: " + blankToDash(value(row.get("supplierName"))) + ".";
        return AiToolAnswer.of(
                answer,
                "purchaseItem.lastPurchased",
                "Last purchased item",
                "Most recent matching purchased item found using token-based item search.",
                rows
        );
    }

    public AiToolAnswer pendingTaskLists() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT tl.id,
                       p.name AS property_name,
                       tl.title,
                       tl.creation_date,
                       tl.due_date,
                       tl.status,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id
                                       AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                GROUP BY tl.id, p.name, tl.title, tl.creation_date, tl.due_date, tl.status
                ORDER BY tl.due_date ASC NULLS LAST, tl.creation_date DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "title", "creationDate", "dueDate", "status", "totalItems", "completedItems");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré listas de tareas pendientes o en progreso.",
                    "taskList.pending",
                    "Pending task lists",
                    "No pending task lists found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas son tus listas de tareas pendientes o en progreso:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | avance: ").append(blankToDash(value(row.get("completedItems"))))
                    .append("/").append(blankToDash(value(row.get("totalItems"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "taskList.pending",
                "Pending task lists",
                "%d pending task lists found.".formatted(rows.size()),
                rows
        );
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
                  AND d.deleted_at IS NULL
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
                  AND d.deleted_at IS NULL
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

    private AiToolAnswer propertiesByStatus(String status, String toolName, String label) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = propertySearchRows(organizationId, null, status, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré propiedades con estado " + status + ".",
                    toolName,
                    label,
                    "No properties found for status " + status + ".",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(status.equals("ACTIVE")
                ? "Estas son tus propiedades activas:"
                : "Estas son tus propiedades inactivas:");
        appendPropertyList(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d properties found for status %s.".formatted(rows.size(), status),
                rows
        );
    }

    private List<Map<String, Object>> propertySearchRows(UUID organizationId, String search, String status, int limit) {
        return query("""
                SELECT p.id, p.name, p.status, p.address, p.description
                FROM properties p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS TEXT))
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                ORDER BY p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("status", status);
                    q.setParameter("limit", limit);
                }, "id", "name", "status", "address", "description");
    }

    private void appendPropertyList(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("name"))))
                    .append(" — estado: ")
                    .append(blankToDash(value(row.get("status"))));
            String address = value(row.get("address"));
            if (!address.isBlank()) {
                answer.append(" | dirección: ").append(address);
            }
        }
    }

    private AiToolAnswer baseCatalog(String tableName, String toolName, String label, String spanishName) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT id, name, description, status
                FROM %s
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                ORDER BY status ASC, name ASC
                LIMIT :limit
                """.formatted(tableName), q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "description", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré " + spanishName + " configurados en tu organización.",
                    toolName,
                    label,
                    "No catalog rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los " + spanishName + " configurados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" — estado: ").append(blankToDash(value(row.get("status"))));
            String description = value(row.get("description"));
            if (!description.isBlank()) {
                answer.append(" | ").append(description);
            }
        }
        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d catalog rows found.".formatted(rows.size()),
                rows
        );
    }

    private Object scalar(String sql, QueryConfigurer configurer) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        return normalizeValue(query.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> query(String sql, QueryConfigurer configurer, String... columns) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        List<Object> resultList = query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object result : resultList) {
            Object[] values = result instanceof Object[] array ? array : new Object[]{result};
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < columns.length; index++) {
                Object value = index < values.length ? values[index] : null;
                row.put(columns[index], normalizeValue(value));
            }
            rows.add(row);
        }
        return rows;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        return value;
    }

    private String extractSearchText(String userQuestion, String... extraStopWords) {
        if (userQuestion == null) {
            return "";
        }
        Set<String> extra = Arrays.stream(extraStopWords)
                .map(this::normalize)
                .collect(Collectors.toSet());
        String cleaned = normalize(userQuestion)
                .replaceAll("[^a-z0-9\\s-]", " ");
        return trimSearch(Arrays.stream(cleaned.split("\\s+"))
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .filter(word -> !SEARCH_STOP_WORDS.contains(word))
                .filter(word -> !extra.contains(word))
                .collect(Collectors.joining(" ")));
    }

    private String trimSearch(String value) {
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.length() > 60 ? cleaned.substring(0, 60).trim() : cleaned;
    }

    private String nullableSearch(String search) {
        return search == null || search.isBlank() ? null : search;
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .toLowerCase(Locale.ROOT)
                .replace("¿", " ")
                .replace("?", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String formatMoney(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "—";
        }
        return "Q " + value;
    }

    private String joinName(Object firstName, Object lastName) {
        return (value(firstName) + " " + value(lastName)).trim();
    }

    @FunctionalInterface
    private interface QueryConfigurer {
        void configure(Query query);
    }
}
