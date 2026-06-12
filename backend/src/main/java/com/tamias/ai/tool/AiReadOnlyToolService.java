package com.tamias.ai.tool;

import com.tamias.ai.dto.AiToolEvidenceResponse;
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
            "registradas", "registrado", "registrados", "se", "son", "subido", "subidos", "tengo",
            "tienes", "tipo", "tu", "un", "una", "usado", "usados", "usaron", "usan", "usa", "uso", "ver", "vez", "y"
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

    public AiToolAnswer scheduledMaintenanceSearch(String userQuestion) {
        return scheduledMaintenanceList("scheduledMaintenance.search", "Scheduled maintenance search", "Mantenimientos programados encontrados:", null, null, extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "programado", "programados", "buscar", "busca", "lista", "listar"));
    }

    public AiToolAnswer upcomingScheduledMaintenance() {
        return scheduledMaintenanceList("scheduledMaintenance.upcoming", "Upcoming scheduled maintenance", "Estos son los próximos mantenimientos programados:", LocalDate.now(), LocalDate.now().plusDays(14), null);
    }

    public AiToolAnswer dueTodayScheduledMaintenance() {
        return scheduledMaintenanceList("scheduledMaintenance.dueToday", "Scheduled maintenance due today", "Estos mantenimientos programados vencen hoy:", LocalDate.now(), LocalDate.now(), null);
    }

    public AiToolAnswer dueThisWeekScheduledMaintenance() {
        return scheduledMaintenanceList("scheduledMaintenance.dueThisWeek", "Scheduled maintenance due this week", "Estos mantenimientos programados vencen esta semana:", LocalDate.now(), LocalDate.now().plusDays(7), null);
    }

    public AiToolAnswer scheduledMaintenanceByProperty(String userQuestion) {
        String search = extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "programado", "programados", "propiedad", "casa", "bungalow", "alojamiento");
        return scheduledMaintenanceList("scheduledMaintenance.byProperty", "Scheduled maintenance by property", "Estos son los mantenimientos programados que encontré por propiedad:", null, null, search);
    }

    public AiToolAnswer scheduledMaintenanceByType(String userQuestion) {
        String search = extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "programado", "programados", "tipo", "categoria", "categorias");
        return scheduledMaintenanceList("scheduledMaintenance.byType", "Scheduled maintenance by type", "Estos son los mantenimientos programados que encontré por tipo/categoría:", null, null, search);
    }

    public AiToolAnswer scheduledMaintenanceByStatus(String userQuestion) {
        String status = resolveScheduledMaintenanceStatus(userQuestion);
        return scheduledMaintenanceList("scheduledMaintenance.byStatus", "Scheduled maintenance by status", "Estos son los mantenimientos programados con estado " + status + ":", null, null, status);
    }

    public AiToolAnswer nextDueScheduledMaintenance(String userQuestion) {
        String search = extractSearchText(userQuestion, "proximo", "proxima", "mantenimiento", "mantenimientos", "programado", "programados", "toca", "vence");
        return scheduledMaintenanceList("scheduledMaintenance.nextDue", "Next due scheduled maintenance", "El próximo mantenimiento programado que encontré es:", LocalDate.now(), null, search, 1);
    }

    public AiToolAnswer scheduledMaintenanceFrequencySummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT sm.frequency,
                       COUNT(*) AS total,
                       COUNT(CASE WHEN sm.status = 'ACTIVE' THEN 1 END) AS active_count,
                       MIN(sm.next_due_date) AS next_due_date
                FROM scheduled_maintenance sm
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                GROUP BY sm.frequency
                ORDER BY total DESC, sm.frequency ASC
                """, q -> q.setParameter("organizationId", organizationId),
                "frequency", "total", "activeCount", "nextDueDate");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos programados para resumir frecuencias.", "scheduledMaintenance.frequencySummary", "Scheduled maintenance frequency summary", "No scheduled maintenance frequencies found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Este es el resumen de frecuencias de mantenimiento programado:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("frequency"))))
                    .append(" | total: ").append(blankToDash(value(row.get("total"))))
                    .append(" | activos: ").append(blankToDash(value(row.get("activeCount"))))
                    .append(" | próximo vencimiento: ").append(blankToDash(value(row.get("nextDueDate"))));
        }
        return AiToolAnswer.of(answer.toString(), "scheduledMaintenance.frequencySummary", "Scheduled maintenance frequency summary", "%d scheduled maintenance frequency rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer scheduledMaintenanceHistory(String userQuestion) {
        String search = extractSearchText(userQuestion, "historial", "historia", "mantenimiento", "mantenimientos", "programado", "programados");
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
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
                  AND (:search IS NULL OR NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', mr.title, mr.description, p.name, mc.name, mt.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  ))
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", nullableSearch(search));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "title", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré historial de mantenimientos relacionado con tu pregunta.", "scheduledMaintenance.history", "Scheduled maintenance history", "No scheduled maintenance history found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Este es el historial de mantenimientos relacionado:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("title"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | fecha: ").append(firstNonBlank(value(row.get("performedAt")), value(row.get("scheduledAt"))))
                    .append(" | costo: ").append(formatMoney(row.get("cost")));
        }
        return AiToolAnswer.of(answer.toString(), "scheduledMaintenance.history", "Scheduled maintenance history", "%d scheduled maintenance history rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer scheduledMaintenanceComplianceSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) AS total,
                       COUNT(CASE WHEN sm.status = 'ACTIVE' THEN 1 END) AS active_count,
                       COUNT(CASE WHEN sm.status = 'ACTIVE' AND sm.next_due_date < :today THEN 1 END) AS overdue_count,
                       COUNT(CASE WHEN sm.status = 'ACTIVE' AND sm.next_due_date = :today THEN 1 END) AS due_today_count,
                       COUNT(CASE WHEN sm.status = 'ACTIVE' AND sm.next_due_date BETWEEN :today AND :weekEnd THEN 1 END) AS due_this_week_count,
                       COUNT(CASE WHEN sm.status = 'PAUSED' THEN 1 END) AS paused_count,
                       COUNT(CASE WHEN sm.status = 'COMPLETED' THEN 1 END) AS completed_count
                FROM scheduled_maintenance sm
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("weekEnd", Date.valueOf(today.plusDays(7)));
                }, "total", "activeCount", "overdueCount", "dueTodayCount", "dueThisWeekCount", "pausedCount", "completedCount");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Así está el cumplimiento de mantenimientos programados:"
                + System.lineSeparator() + "- Total: " + blankToDash(value(row.get("total")))
                + System.lineSeparator() + "- Activos: " + blankToDash(value(row.get("activeCount")))
                + System.lineSeparator() + "- Vencidos: " + blankToDash(value(row.get("overdueCount")))
                + System.lineSeparator() + "- Vencen hoy: " + blankToDash(value(row.get("dueTodayCount")))
                + System.lineSeparator() + "- Vencen esta semana: " + blankToDash(value(row.get("dueThisWeekCount")))
                + System.lineSeparator() + "- Pausados: " + blankToDash(value(row.get("pausedCount")))
                + System.lineSeparator() + "- Completados: " + blankToDash(value(row.get("completedCount")));
        return AiToolAnswer.of(answer, "scheduledMaintenance.complianceSummary", "Scheduled maintenance compliance summary", "Scheduled maintenance compliance counters were calculated.", rows);
    }

    private AiToolAnswer scheduledMaintenanceList(String toolName, String label, String emptyOrIntro, LocalDate from, LocalDate to, String search) {
        return scheduledMaintenanceList(toolName, label, emptyOrIntro, from, to, search, DEFAULT_LIMIT);
    }

    private AiToolAnswer scheduledMaintenanceList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT sm.id,
                       p.name AS property_name,
                       sm.title,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
                       sm.frequency,
                       sm.interval_value,
                       sm.next_due_date,
                       sm.estimated_cost,
                       sm.status
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = sm.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = sm.maintenance_type_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND (:fromDate IS NULL OR sm.next_due_date >= CAST(:fromDate AS DATE))
                  AND (:toDate IS NULL OR sm.next_due_date <= CAST(:toDate AS DATE))
                  AND (:search IS NULL OR sm.status = CAST(:search AS TEXT) OR NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', sm.title, sm.description, p.name, mc.name, mt.name, sm.frequency, sm.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  ))
                ORDER BY sm.next_due_date ASC, sm.title ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", from == null ? null : Date.valueOf(from));
                    q.setParameter("toDate", to == null ? null : Date.valueOf(to));
                    q.setParameter("search", nullableSearch(search));
                    q.setParameter("limit", limit);
                }, "id", "propertyName", "title", "categoryName", "typeName", "frequency", "intervalValue", "nextDueDate", "estimatedCost", "status");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos programados que coincidan con tu pregunta.", toolName, label, "No scheduled maintenance rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("title"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("nextDueDate"))))
                    .append(" | frecuencia: ").append(blankToDash(value(row.get("frequency"))))
                    .append(" cada ").append(blankToDash(value(row.get("intervalValue"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
            String category = value(row.get("categoryName"));
            String type = value(row.get("typeName"));
            if (!category.isBlank() || !type.isBlank()) {
                answer.append(" | ").append(blankToDash(category));
                if (!type.isBlank()) {
                    answer.append(" / ").append(type);
                }
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d scheduled maintenance rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationsToday() {
        return reservationList("reservation.today", "Reservations today", "Estas son las reservaciones con movimiento hoy:", LocalDate.now(), LocalDate.now(), null);
    }

    public AiToolAnswer currentReservations() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        return reservationList("reservation.current", "Current reservations", "Estas son las reservaciones actualmente en curso:", today, today, null, DEFAULT_LIMIT, true);
    }

    public AiToolAnswer reservationsThisWeek() {
        return reservationList("reservation.thisWeek", "Reservations this week", "Estas son las reservaciones de esta semana:", LocalDate.now(), LocalDate.now().plusDays(7), null);
    }

    public AiToolAnswer reservationsThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return reservationList("reservation.thisMonth", "Reservations this month", "Estas son las reservaciones de este mes:", start, end, null);
    }

    public AiToolAnswer reservationsByProperty(String userQuestion) {
        String search = extractSearchText(userQuestion, "reservacion", "reservaciones", "reserva", "reservas", "propiedad", "casa", "bungalow", "alojamiento");
        return reservationList("reservation.byProperty", "Reservations by property", "Estas son las reservaciones que encontré por propiedad:", null, null, search);
    }

    public AiToolAnswer reservationsByGuest(String userQuestion) {
        String search = extractSearchText(userQuestion, "reservacion", "reservaciones", "reserva", "reservas", "huesped", "huespedes", "cliente", "clientes");
        return reservationList("reservation.byGuest", "Reservations by guest", "Estas son las reservaciones que encontré por huésped:", null, null, search);
    }

    public AiToolAnswer reservationsByStatus(String userQuestion) {
        String status = resolveReservationStatus(userQuestion);
        return reservationList("reservation.byStatus", "Reservations by status", "Estas son las reservaciones con estado " + status + ":", null, null, status);
    }

    public AiToolAnswer reservationsByPlatform(String userQuestion) {
        String search = extractSearchText(userQuestion, "reservacion", "reservaciones", "reserva", "reservas", "plataforma", "platform", "airbnb", "booking");
        return reservationList("reservation.byPlatform", "Reservations by platform", "Estas son las reservaciones que encontré por plataforma:", null, null, search);
    }

    public AiToolAnswer reservationSearch(String userQuestion) {
        String search = extractSearchText(userQuestion, "reservacion", "reservaciones", "reserva", "reservas", "buscar", "busca", "lista", "listar");
        return reservationList("reservation.search", "Reservation search", "Estas son las reservaciones que encontré:", null, null, search);
    }

    public AiToolAnswer nextCheckIn() {
        return reservationList("reservation.nextCheckIn", "Next check-in", "La próxima llegada registrada es:", LocalDate.now(), null, null, 1, false);
    }

    public AiToolAnswer nextCheckOut() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query(reservationBaseSql("r.check_out >= :fromDate", "r.check_out ASC", 1), q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("fromDate", Date.valueOf(today));
            q.setParameter("limit", 1);
        }, reservationColumns());
        return reservationRowsAnswer(rows, "reservation.nextCheckOut", "Next check-out", "La próxima salida registrada es:", "No encontré próximas salidas registradas.");
    }

    public AiToolAnswer reservationCalendarEvents() {
        return reservationList("reservation.calendarEvents", "Reservation calendar events", "Estos son eventos de calendario de reservaciones para esta semana:", LocalDate.now(), LocalDate.now().plusDays(7), null);
    }

    public AiToolAnswer reservationRevenueSummary(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) AS reservation_count,
                       COALESCE(SUM(r.reservation_value), 0) AS total_revenue,
                       COALESCE(AVG(r.reservation_value), 0) AS average_revenue,
                       COALESCE(SUM(r.check_out - r.check_in), 0) AS total_nights
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND (:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE))
                  AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", range[0] == null ? null : Date.valueOf(range[0]));
                    q.setParameter("toDate", range[1] == null ? null : Date.valueOf(range[1]));
                }, "reservationCount", "totalRevenue", "averageRevenue", "totalNights");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Resumen de ingresos de reservaciones:"
                + System.lineSeparator() + "- Reservaciones: " + blankToDash(value(row.get("reservationCount")))
                + System.lineSeparator() + "- Ingresos totales: " + formatMoney(row.get("totalRevenue"))
                + System.lineSeparator() + "- Promedio por reservación: " + formatMoney(row.get("averageRevenue"))
                + System.lineSeparator() + "- Noches reservadas: " + blankToDash(value(row.get("totalNights")));
        return AiToolAnswer.of(answer, "reservation.revenueSummary", "Reservation revenue summary", "Reservation revenue counters were calculated.", rows);
    }

    public AiToolAnswer reservationNightsSummary(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(SUM(r.check_out - r.check_in), 0) AS total_nights,
                       COUNT(*) AS reservation_count
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND (:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE))
                  AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", range[0] == null ? null : Date.valueOf(range[0]));
                    q.setParameter("toDate", range[1] == null ? null : Date.valueOf(range[1]));
                }, "totalNights", "reservationCount");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Resumen de noches reservadas:"
                + System.lineSeparator() + "- Noches reservadas: " + blankToDash(value(row.get("totalNights")))
                + System.lineSeparator() + "- Reservaciones consideradas: " + blankToDash(value(row.get("reservationCount")));
        return AiToolAnswer.of(answer, "reservation.nightsSummary", "Reservation nights summary", "Reservation nights summary was calculated.", rows);
    }

    public AiToolAnswer reservationGuestCountSummary(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT rg.guest_id) AS unique_guests,
                       COUNT(rg.id) AS guest_reservation_links,
                       COUNT(DISTINCT r.id) AS reservation_count
                FROM reservations r
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND (:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE))
                  AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", range[0] == null ? null : Date.valueOf(range[0]));
                    q.setParameter("toDate", range[1] == null ? null : Date.valueOf(range[1]));
                }, "uniqueGuests", "guestReservationLinks", "reservationCount");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Resumen de huéspedes en reservaciones:"
                + System.lineSeparator() + "- Huéspedes únicos: " + blankToDash(value(row.get("uniqueGuests")))
                + System.lineSeparator() + "- Asignaciones de huéspedes a reservas: " + blankToDash(value(row.get("guestReservationLinks")))
                + System.lineSeparator() + "- Reservaciones consideradas: " + blankToDash(value(row.get("reservationCount")));
        return AiToolAnswer.of(answer, "reservation.guestCountSummary", "Reservation guest count summary", "Guest counts for reservations were calculated.", rows);
    }

    public AiToolAnswer reservationOccupancySummary(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT p.name AS property_name,
                       COUNT(r.id) AS reservation_count,
                       COALESCE(SUM(r.check_out - r.check_in), 0) AS reserved_nights
                FROM properties p
                LEFT JOIN reservations r ON r.property_id = p.id
                                       AND r.organization_id = p.organization_id
                                       AND r.deleted_at IS NULL
                                       AND r.status = 'ACTIVE'
                                       AND (:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE))
                                       AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name
                ORDER BY reserved_nights DESC, reservation_count DESC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", range[0] == null ? null : Date.valueOf(range[0]));
                    q.setParameter("toDate", range[1] == null ? null : Date.valueOf(range[1]));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "propertyName", "reservationCount", "reservedNights");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré propiedades para calcular ocupación.", "reservation.occupancySummary", "Reservation occupancy summary", "No occupancy rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Resumen de ocupación por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | reservaciones: ").append(blankToDash(value(row.get("reservationCount"))))
                    .append(" | noches: ").append(blankToDash(value(row.get("reservedNights"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservation.occupancySummary", "Reservation occupancy summary", "%d occupancy rows calculated.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationGapsBetweenReservations() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                WITH ordered AS (
                    SELECT r.id,
                           p.name AS property_name,
                           r.check_in,
                           r.check_out,
                           LEAD(r.check_in) OVER (PARTITION BY r.property_id ORDER BY r.check_in) AS next_check_in
                    FROM reservations r
                    JOIN properties p ON p.id = r.property_id
                    WHERE r.organization_id = :organizationId
                      AND r.deleted_at IS NULL
                      AND r.status = 'ACTIVE'
                      AND r.check_out >= CURRENT_DATE
                )
                SELECT property_name,
                       check_out,
                       next_check_in,
                       (next_check_in - check_out) AS gap_days
                FROM ordered
                WHERE next_check_in IS NOT NULL
                  AND next_check_in > check_out
                ORDER BY check_out ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "propertyName", "checkOut", "nextCheckIn", "gapDays");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré espacios libres entre reservaciones próximas.", "reservation.gapsBetweenReservations", "Reservation gaps between reservations", "No future reservation gaps found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son espacios libres entre reservaciones próximas:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | salida: ").append(blankToDash(value(row.get("checkOut"))))
                    .append(" | próxima llegada: ").append(blankToDash(value(row.get("nextCheckIn"))))
                    .append(" | días libres: ").append(blankToDash(value(row.get("gapDays"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservation.gapsBetweenReservations", "Reservation gaps between reservations", "%d reservation gap rows found.".formatted(rows.size()), rows);
    }

    private AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search) {
        return reservationList(toolName, label, intro, from, to, search, DEFAULT_LIMIT, false);
    }

    private AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit, boolean currentOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String dateFilter = currentOnly
                ? "r.check_in <= :fromDate AND r.check_out > :fromDate"
                : "(:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE)) AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))";
        List<Map<String, Object>> rows = query("""
                SELECT r.id,
                       p.name AS property_name,
                       COALESCE(pl.name, '') AS platform_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.reservation_value,
                       r.status,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests,
                       COALESCE(COUNT(g.id), 0) AS guest_count
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN platforms pl ON pl.id = r.platform_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND """ + dateFilter + """
                  AND (:search IS NULL OR r.status = CAST(:search AS TEXT) OR NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', p.name, pl.name, r.reservation_code, r.observations, r.status, g.full_name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  ))
                GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status
                ORDER BY r.check_in ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", from == null ? null : Date.valueOf(from));
                    q.setParameter("toDate", to == null ? null : Date.valueOf(to));
                    q.setParameter("search", nullableSearch(search));
                    q.setParameter("limit", limit);
                }, reservationColumns());
        return reservationRowsAnswer(rows, toolName, label, intro, "No encontré reservaciones que coincidan con tu pregunta.");
    }

    private AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
        return reservationList(toolName, label, intro, from, to, search, limit, false);
    }

    private String reservationBaseSql(String whereClause, String orderBy, int limit) {
        return """
                SELECT r.id,
                       p.name AS property_name,
                       COALESCE(pl.name, '') AS platform_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.reservation_value,
                       r.status,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests,
                       COALESCE(COUNT(g.id), 0) AS guest_count
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN platforms pl ON pl.id = r.platform_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND """ + whereClause + """
                GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status
                ORDER BY """ + orderBy + """
                LIMIT :limit
                """;
    }

    private String[] reservationColumns() {
        return new String[]{"id", "propertyName", "platformName", "reservationCode", "checkIn", "checkOut", "reservationValue", "status", "guests", "guestCount"};
    }

    private AiToolAnswer reservationRowsAnswer(List<Map<String, Object>> rows, String toolName, String label, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, label, "No reservation rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | valor: ").append(formatMoney(row.get("reservationValue")));
            String platform = value(row.get("platformName"));
            if (!platform.isBlank()) {
                answer.append(" | plataforma: ").append(platform);
            }
            String guests = value(row.get("guests"));
            if (!guests.isBlank()) {
                answer.append(" | huéspedes: ").append(guests);
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d reservation rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer guestSearch(String userQuestion) {
        String search = extractSearchText(userQuestion, "huesped", "huespedes", "cliente", "clientes", "buscar", "busca", "lista", "listar");
        return guestList("guest.search", "Guest search", "Estos son los huéspedes que encontré:", search, false, false);
    }

    public AiToolAnswer guestsByReservation(String userQuestion) {
        String search = extractSearchText(userQuestion, "huesped", "huespedes", "reservacion", "reservaciones", "reserva", "reservas");
        return guestReservationList("guest.byReservation", "Guests by reservation", "Estos son los huéspedes asociados a reservaciones:", search, false);
    }

    public AiToolAnswer recentGuests() {
        return guestReservationList("guest.recent", "Recent guests", "Estos son los huéspedes recientes:", null, false);
    }

    public AiToolAnswer returningGuests() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT g.id,
                       g.full_name,
                       COUNT(DISTINCT rg.reservation_id) AS reservation_count,
                       MAX(r.check_in) AS last_check_in
                FROM guests g
                JOIN reservation_guests rg ON rg.guest_id = g.id AND rg.organization_id = g.organization_id
                JOIN reservations r ON r.id = rg.reservation_id AND r.organization_id = g.organization_id
                WHERE g.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                  AND r.deleted_at IS NULL
                GROUP BY g.id, g.full_name
                HAVING COUNT(DISTINCT rg.reservation_id) > 1
                ORDER BY reservation_count DESC, last_check_in DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "fullName", "reservationCount", "lastCheckIn");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré huéspedes recurrentes todavía.", "guest.returningGuests", "Returning guests", "No returning guests found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos huéspedes aparecen en más de una reservación:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | reservaciones: ").append(blankToDash(value(row.get("reservationCount"))))
                    .append(" | última llegada: ").append(blankToDash(value(row.get("lastCheckIn"))));
        }
        return AiToolAnswer.of(answer.toString(), "guest.returningGuests", "Returning guests", "%d returning guests found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer upcomingGuests() {
        return guestReservationList("guest.upcomingGuests", "Upcoming guests", "Estos huéspedes tienen llegada próxima:", null, true);
    }

    public AiToolAnswer guestCountByDateRange(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT g.id) AS unique_guests,
                       COUNT(rg.id) AS guest_links
                FROM reservation_guests rg
                JOIN guests g ON g.id = rg.guest_id
                JOIN reservations r ON r.id = rg.reservation_id
                WHERE rg.organization_id = :organizationId
                  AND g.organization_id = :organizationId
                  AND r.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                  AND r.deleted_at IS NULL
                  AND (:fromDate IS NULL OR r.check_in >= CAST(:fromDate AS DATE))
                  AND (:toDate IS NULL OR r.check_in <= CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", range[0] == null ? null : Date.valueOf(range[0]));
                    q.setParameter("toDate", range[1] == null ? null : Date.valueOf(range[1]));
                }, "uniqueGuests", "guestLinks");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Conteo de huéspedes:"
                + System.lineSeparator() + "- Huéspedes únicos: " + blankToDash(value(row.get("uniqueGuests")))
                + System.lineSeparator() + "- Asignaciones a reservaciones: " + blankToDash(value(row.get("guestLinks")));
        return AiToolAnswer.of(answer, "guest.countByDateRange", "Guest count by date range", "Guest count by date range was calculated.", rows);
    }

    private AiToolAnswer guestList(String toolName, String label, String intro, String search, boolean upcomingOnly, boolean recentOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT g.id,
                       g.full_name,
                       g.status,
                       COUNT(DISTINCT rg.reservation_id) AS reservation_count,
                       MAX(r.check_in) AS last_check_in
                FROM guests g
                LEFT JOIN reservation_guests rg ON rg.guest_id = g.id AND rg.organization_id = g.organization_id
                LEFT JOIN reservations r ON r.id = rg.reservation_id AND r.organization_id = g.organization_id AND r.deleted_at IS NULL
                WHERE g.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                  AND (:search IS NULL OR NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, g.notes, g.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  ))
                  AND (:upcomingOnly = FALSE OR r.check_in >= CURRENT_DATE)
                GROUP BY g.id, g.full_name, g.status
                ORDER BY MAX(r.check_in) DESC NULLS LAST, g.full_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", nullableSearch(search));
                    q.setParameter("upcomingOnly", upcomingOnly);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "fullName", "status", "reservationCount", "lastCheckIn");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré huéspedes que coincidan con tu pregunta.", toolName, label, "No guests found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | reservaciones: ").append(blankToDash(value(row.get("reservationCount"))))
                    .append(" | última llegada: ").append(blankToDash(value(row.get("lastCheckIn"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d guest rows found.".formatted(rows.size()), rows);
    }

    private AiToolAnswer guestReservationList(String toolName, String label, String intro, String search, boolean upcomingOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT g.id,
                       g.full_name,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       rg.is_primary
                FROM reservation_guests rg
                JOIN guests g ON g.id = rg.guest_id
                JOIN reservations r ON r.id = rg.reservation_id
                JOIN properties p ON p.id = r.property_id
                WHERE rg.organization_id = :organizationId
                  AND g.organization_id = :organizationId
                  AND r.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                  AND r.deleted_at IS NULL
                  AND (:upcomingOnly = FALSE OR r.check_in >= CURRENT_DATE)
                  AND (:search IS NULL OR NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  ))
                ORDER BY r.check_in DESC, rg.is_primary DESC, g.full_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("upcomingOnly", upcomingOnly);
                    q.setParameter("search", nullableSearch(search));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "fullName", "propertyName", "reservationCode", "checkIn", "checkOut", "isPrimary");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré huéspedes asociados a reservaciones que coincidan con tu pregunta.", toolName, label, "No reservation guest rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))));
            String code = value(row.get("reservationCode"));
            if (!code.isBlank()) {
                answer.append(" | código: ").append(code);
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d reservation guest rows found.".formatted(rows.size()), rows);
    }

    private String resolveScheduledMaintenanceStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "pausado", "pausados", "pausada", "pausadas", "paused")) {
            return "PAUSED";
        }
        if (containsAny(normalized, "completado", "completados", "completada", "completadas", "completed")) {
            return "COMPLETED";
        }
        if (containsAny(normalized, "cancelado", "cancelados", "cancelada", "canceladas", "cancelled", "canceled")) {
            return "CANCELLED";
        }
        return "ACTIVE";
    }

    private String resolveReservationStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "cancelado", "cancelados", "cancelada", "canceladas", "cancelled", "canceled")) {
            return "CANCELLED";
        }
        if (containsAny(normalized, "eliminado", "eliminados", "deleted")) {
            return "DELETED";
        }
        return "ACTIVE";
    }

    private LocalDate[] resolveDateRange(String userQuestion) {
        String normalized = normalize(userQuestion);
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "hoy")) {
            return new LocalDate[]{today, today};
        }
        if (containsAny(normalized, "semana")) {
            return new LocalDate[]{today, today.plusDays(7)};
        }
        if (containsAny(normalized, "mes")) {
            LocalDate start = today.withDayOfMonth(1);
            return new LocalDate[]{start, start.plusMonths(1).minusDays(1)};
        }
        return new LocalDate[]{null, null};
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? blankToDash(second) : first;
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
                             AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                ORDER BY CASE
                         WHEN CAST(:search AS TEXT) IS NOT NULL
                              AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') = CAST(:search AS TEXT) THEN 0
                         WHEN CAST(:search AS TEXT) IS NOT NULL
                              AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', CAST(:search AS TEXT), '%') THEN 1
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



    public AiToolAnswer inventorySearch(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "inventario", "inventory", "item", "items", "supply", "supplies", "repuesto", "repuestos", "material", "materiales", "registrado", "registrados"
        ));
        List<Map<String, Object>> rows = query("""
                SELECT ii.id,
                       ii.name,
                       ii.description,
                       ii.status,
                       ii.item_type,
                       ii.unit,
                       ii.internal_code,
                       ii.barcode,
                       ii.available_for_maintenance,
                       ii.available_for_reservations,
                       ii.available_for_purchases
                FROM inventory_items ii
                WHERE ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND translate(LOWER(CONCAT_WS(' ', ii.name, ii.description, ii.internal_code, ii.barcode, ii.item_type)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                ORDER BY ii.status ASC, ii.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "description", "status", "itemType", "unit", "internalCode", "barcode", "availableForMaintenance", "availableForReservations", "availableForPurchases");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré items de inventario registrados en tu organización."
                            : "No encontré items de inventario relacionados con “" + search + "”.",
                    "inventory.search",
                    "Inventory search",
                    "No inventory items found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos son los items de inventario que encontré:" : "Encontré estos items relacionados con “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                    .append(" | unidad: ").append(blankToDash(value(row.get("unit"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
            List<String> uses = new ArrayList<>();
            if ("true".equalsIgnoreCase(value(row.get("availableForMaintenance")))) {
                uses.add("mantenimiento");
            }
            if ("true".equalsIgnoreCase(value(row.get("availableForReservations")))) {
                uses.add("reservaciones");
            }
            if ("true".equalsIgnoreCase(value(row.get("availableForPurchases")))) {
                uses.add("compras");
            }
            if (!uses.isEmpty()) {
                answer.append(" | usable en: ").append(String.join(", ", uses));
            }
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.search",
                "Inventory search",
                "%d inventory items found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryFrequentlyUsed() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT source.item_name,
                       source.inventory_item_id,
                       SUM(source.usage_count) AS usage_count,
                       SUM(source.total_quantity) AS total_quantity,
                       COALESCE(MAX(ii.item_type), 'SNAPSHOT_ONLY') AS item_type
                FROM (
                    SELECT COALESCE(rs.inventory_item_id, NULL) AS inventory_item_id,
                           rs.item_name_snapshot AS item_name,
                           COUNT(*) AS usage_count,
                           COALESCE(SUM(rs.quantity), 0) AS total_quantity
                    FROM reservation_supplies rs
                    WHERE rs.organization_id = :organizationId
                    GROUP BY rs.inventory_item_id, rs.item_name_snapshot
                    UNION ALL
                    SELECT COALESCE(mri.inventory_item_id, NULL) AS inventory_item_id,
                           mri.item_name_snapshot AS item_name,
                           COUNT(*) AS usage_count,
                           COALESCE(SUM(mri.quantity), 0) AS total_quantity
                    FROM maintenance_record_items mri
                    WHERE mri.organization_id = :organizationId
                    GROUP BY mri.inventory_item_id, mri.item_name_snapshot
                    UNION ALL
                    SELECT COALESCE(pi.inventory_item_id, NULL) AS inventory_item_id,
                           pi.item_name_snapshot AS item_name,
                           COUNT(*) AS usage_count,
                           COALESCE(SUM(pi.quantity), 0) AS total_quantity
                    FROM purchase_items pi
                    JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                    WHERE pi.organization_id = :organizationId
                      AND pl.organization_id = :organizationId
                      AND pl.deleted_at IS NULL
                      AND pi.purchased = TRUE
                    GROUP BY pi.inventory_item_id, pi.item_name_snapshot
                ) source
                LEFT JOIN inventory_items ii ON ii.id = source.inventory_item_id
                                           AND ii.organization_id = :organizationId
                                           AND ii.deleted_at IS NULL
                GROUP BY source.item_name, source.inventory_item_id
                ORDER BY usage_count DESC, total_quantity DESC, source.item_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "itemName", "inventoryItemId", "usageCount", "totalQuantity", "itemType");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré uso de items en reservaciones, mantenimientos o compras marcadas como compradas.",
                    "inventory.getFrequentlyUsed",
                    "Frequently used inventory items",
                    "No inventory usage rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los items más usados/comprados en TAMIAS:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | usos/registros: ").append(blankToDash(value(row.get("usageCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.getFrequentlyUsed",
                "Frequently used inventory items",
                "%d frequently used inventory rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryUnusedItems() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT ii.id,
                       ii.name,
                       ii.item_type,
                       ii.unit,
                       ii.status,
                       ii.available_for_maintenance,
                       ii.available_for_reservations,
                       ii.available_for_purchases
                FROM inventory_items ii
                WHERE ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM reservation_supplies rs
                      WHERE rs.organization_id = ii.organization_id
                        AND rs.inventory_item_id = ii.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM maintenance_record_items mri
                      WHERE mri.organization_id = ii.organization_id
                        AND mri.inventory_item_id = ii.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM purchase_items pi
                      JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                      WHERE pi.organization_id = ii.organization_id
                        AND pl.organization_id = ii.organization_id
                        AND pl.deleted_at IS NULL
                        AND pi.inventory_item_id = ii.id
                  )
                ORDER BY ii.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "itemType", "unit", "status", "availableForMaintenance", "availableForReservations", "availableForPurchases");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré items de inventario activos sin uso registrado. Todos parecen tener relación con compras, reservaciones o mantenimientos, o no hay items registrados.",
                    "inventory.getUnusedItems",
                    "Unused inventory items",
                    "No unused inventory items found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos items de inventario no tienen uso registrado todavía:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.getUnusedItems",
                "Unused inventory items",
                "%d unused inventory items found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryReservationUsage(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "se", "ha", "han", "usado", "usados", "usaron", "usa", "usan", "uso", "reservacion", "reservaciones", "reserva", "reservas", "supply", "supplies", "item", "items"));
        List<Map<String, Object>> rows = query("""
                SELECT rs.item_name_snapshot,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(rs.quantity), 0) AS total_quantity,
                       COALESCE(MAX(rs.unit), '') AS unit,
                       MAX(r.check_in) AS last_check_in,
                       COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id
                                   AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id
                                 AND p.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND translate(LOWER(CONCAT_WS(' ', rs.item_name_snapshot, rs.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                GROUP BY rs.item_name_snapshot
                ORDER BY usage_count DESC, last_check_in DESC NULLS LAST, rs.item_name_snapshot ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "itemName", "usageCount", "totalQuantity", "unit", "lastCheckIn", "properties");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré supplies registrados en reservaciones."
                            : "No encontré uso en reservaciones para “" + search + "”.",
                    "inventory.getItemsUsedInReservations",
                    "Inventory items used in reservations",
                    "No reservation supply usage found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos supplies aparecen en reservaciones:" : "Encontré este uso en reservaciones para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | registros: ").append(blankToDash(value(row.get("usageCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | propiedades: ").append(blankToDash(value(row.get("properties"))))
                    .append(" | último check-in asociado: ").append(blankToDash(value(row.get("lastCheckIn"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.getItemsUsedInReservations",
                "Inventory items used in reservations",
                "%d reservation supply usage rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryPurchaseUsage(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "se", "ha", "han", "usado", "usados", "uso", "comprado", "compre", "compras", "compra", "historial", "item", "items", "producto", "productos"));
        List<Map<String, Object>> rows = query("""
                SELECT pi.item_name_snapshot,
                       COUNT(*) AS purchase_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(MAX(pi.unit), '') AS unit,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_estimated_cost,
                       MAX(pl.purchase_date) AS last_purchase_date,
                       COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                LEFT JOIN properties p ON p.id = pl.property_id
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
                             AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                GROUP BY pi.item_name_snapshot
                ORDER BY last_purchase_date DESC NULLS LAST, purchase_count DESC, pi.item_name_snapshot ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalEstimatedCost", "lastPurchaseDate", "properties");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré items marcados como comprados."
                            : "No encontré compras marcadas como compradas para “" + search + "”.",
                    "inventory.getItemsUsedInPurchases",
                    "Inventory items used in purchases",
                    "No purchased item usage found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos items aparecen en compras marcadas como compradas:" : "Encontré compras para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | compras: ").append(blankToDash(value(row.get("purchaseCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | costo estimado total: ").append(formatMoney(row.get("totalEstimatedCost")))
                    .append(" | última compra: ").append(blankToDash(value(row.get("lastPurchaseDate"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.getItemsUsedInPurchases",
                "Inventory items used in purchases",
                "%d purchased item usage rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer inventoryMaintenanceUsage(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "se", "ha", "han", "usado", "usados", "usaron", "usa", "usan", "uso", "mantenimiento", "mantenimientos", "item", "items", "repuesto", "repuestos", "material", "materiales"));
        List<Map<String, Object>> rows = query("""
                SELECT mri.item_name_snapshot,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(mri.quantity), 0) AS total_quantity,
                       COALESCE(MAX(mri.unit), '') AS unit,
                       MAX(COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)) AS last_used_at,
                       COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties
                FROM maintenance_record_items mri
                JOIN maintenance_records mr ON mr.id = mri.maintenance_record_id
                                           AND mr.organization_id = mri.organization_id
                JOIN properties p ON p.id = mr.property_id
                                 AND p.organization_id = mri.organization_id
                WHERE mri.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND translate(LOWER(CONCAT_WS(' ', mri.item_name_snapshot, mri.notes, mr.title, mr.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                GROUP BY mri.item_name_snapshot
                ORDER BY last_used_at DESC NULLS LAST, usage_count DESC, mri.item_name_snapshot ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "itemName", "usageCount", "totalQuantity", "unit", "lastUsedAt", "properties");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré items usados en mantenimientos."
                            : "No encontré uso en mantenimientos para “" + search + "”.",
                    "inventory.getItemsUsedInMaintenance",
                    "Inventory items used in maintenance",
                    "No maintenance item usage found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos items aparecen en mantenimientos:" : "Encontré uso en mantenimientos para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | registros: ").append(blankToDash(value(row.get("usageCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | propiedades: ").append(blankToDash(value(row.get("properties"))))
                    .append(" | último uso: ").append(blankToDash(value(row.get("lastUsedAt"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                "inventory.getItemsUsedInMaintenance",
                "Inventory items used in maintenance",
                "%d maintenance item usage rows found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer maintenanceSearch(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "buscar", "busca", "lista", "listar", "recientes", "reciente", "realizados", "realizado"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, search, null, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré mantenimientos registrados." : "No encontré mantenimientos relacionados con “" + search + "”.",
                    "maintenance.search",
                    "Maintenance search",
                    "No maintenance records found.",
                    List.of()
            );
        }
        return maintenanceRowsAnswer(rows, "maintenance.search", "Maintenance search", search == null ? "Estos son los mantenimientos más recientes:" : "Encontré estos mantenimientos relacionados con “" + search + "”:");
    }

    public AiToolAnswer recentMaintenance() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos recientes.", "maintenance.recent", "Recent maintenance", "No recent maintenance records found.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.recent", "Recent maintenance", "Estos son los mantenimientos más recientes:");
    }

    public AiToolAnswer maintenanceByStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        String status = null;
        if (containsAny(normalized, "completado", "completados", "realizado", "realizados", "completed")) {
            status = "COMPLETED";
        } else if (containsAny(normalized, "pendiente", "pendientes", "pending")) {
            status = "PENDING";
        } else if (containsAny(normalized, "cancelado", "cancelados", "cancelled", "canceled")) {
            status = "CANCELLED";
        } else if (containsAny(normalized, "en progreso", "progreso", "in progress")) {
            status = "IN_PROGRESS";
        }
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, null, status, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos con estado " + blankToDash(status) + ".", "maintenance.byStatus", "Maintenance by status", "No maintenance records found for status.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byStatus", "Maintenance by status", "Estos son los mantenimientos con estado " + status + ":");
    }

    public AiToolAnswer maintenanceByProperty(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "propiedad", "propiedades", "casa", "bungalow", "alojamiento", "de", "del"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, search, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos para esa propiedad.", "maintenance.byProperty", "Maintenance by property", "No maintenance records found for property.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byProperty", "Maintenance by property", "Estos son los mantenimientos que encontré para esa propiedad:");
    }

    public AiToolAnswer maintenanceByCategoryOrType(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "categoria", "categorias", "tipo", "tipos", "de", "del"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos para esa categoría o tipo.", "maintenance.byCategoryOrType", "Maintenance by category or type", "No maintenance records found for category/type.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byCategoryOrType", "Maintenance by category or type", "Estos son los mantenimientos que encontré por categoría/tipo:");
    }

    public AiToolAnswer maintenanceCostSummary(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        boolean thisMonth = containsAny(normalize(userQuestion), "este mes", "mes actual");
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost,
                       COALESCE(MAX(mr.cost), 0) AS max_cost,
                       COALESCE(MIN(mr.cost), 0) AS min_cost
                FROM maintenance_records mr
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                  AND (CAST(:fromDate AS DATE) IS NULL OR COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) >= CAST(:fromDate AS DATE))
                  AND (CAST(:toDate AS DATE) IS NULL OR COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) < CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", thisMonth ? Date.valueOf(monthStart) : null);
                    q.setParameter("toDate", thisMonth ? Date.valueOf(nextMonthStart) : null);
                }, "maintenanceCount", "totalCost", "averageCost", "maxCost", "minCost");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String period = thisMonth ? "este mes" : "en todos los mantenimientos con costo";
        String answer = "El resumen de costos de mantenimiento " + period + " es:\n"
                + "- Mantenimientos con costo: " + blankToDash(value(row.get("maintenanceCount"))) + "\n"
                + "- Total: " + formatMoney(row.get("totalCost")) + "\n"
                + "- Promedio: " + formatMoney(row.get("averageCost")) + "\n"
                + "- Máximo: " + formatMoney(row.get("maxCost")) + "\n"
                + "- Mínimo: " + formatMoney(row.get("minCost"));
        return AiToolAnswer.of(answer, "maintenance.costSummary", "Maintenance cost summary", "Maintenance cost summary was calculated.", rows);
    }

    public AiToolAnswer maintenanceCostByProperty() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT p.name AS property_name,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY p.name
                ORDER BY total_cost DESC, maintenance_count DESC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "propertyName", "maintenanceCount", "totalCost", "averageCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento por propiedad.", "maintenance.costByProperty", "Maintenance cost by property", "No maintenance cost rows found by property.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageCost")));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByProperty", "Maintenance cost by property", "%d maintenance cost rows by property found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceCostByCategory() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(mc.name, 'Sin categoría') AS category_name,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost
                FROM maintenance_records mr
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY COALESCE(mc.name, 'Sin categoría')
                ORDER BY total_cost DESC, maintenance_count DESC, category_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "categoryName", "maintenanceCount", "totalCost", "averageCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento por categoría.", "maintenance.costByCategory", "Maintenance cost by category", "No maintenance cost rows found by category.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por categoría:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageCost")));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByCategory", "Maintenance cost by category", "%d maintenance cost rows by category found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceCostByMonth() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT TO_CHAR(DATE_TRUNC('month', COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)), 'YYYY-MM') AS month,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost
                FROM maintenance_records mr
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY DATE_TRUNC('month', COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at))
                ORDER BY month DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "month", "maintenanceCount", "totalCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento agrupados por mes.", "maintenance.costByMonth", "Maintenance cost by month", "No maintenance cost rows found by month.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por mes:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("month"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByMonth", "Maintenance cost by month", "%d maintenance cost rows by month found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceImagesSummary(boolean withoutImages) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.status,
                       COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS maintenance_date,
                       COUNT(mri.id) AS image_count
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                                                       AND mri.organization_id = mr.organization_id
                                                       AND mri.deleted_at IS NULL
                                                       AND mri.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                GROUP BY mr.id, p.name, mr.title, mr.status, COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)
                HAVING CAST(:withoutImages AS BOOLEAN) = FALSE OR COUNT(mri.id) = 0
                ORDER BY image_count DESC, maintenance_date DESC NULLS LAST
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("withoutImages", withoutImages);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "title", "status", "maintenanceDate", "imageCount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    withoutImages ? "No encontré mantenimientos sin evidencia fotográfica." : "No encontré metadata de imágenes en mantenimientos.",
                    withoutImages ? "maintenance.withoutImages" : "maintenance.withImages",
                    withoutImages ? "Maintenance without images" : "Maintenance with images",
                    "No maintenance image metadata rows found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(withoutImages ? "Estos mantenimientos no tienen imágenes activas:" : "Estos mantenimientos tienen metadata de imágenes:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("maintenanceDate"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                withoutImages ? "maintenance.withoutImages" : "maintenance.withImages",
                withoutImages ? "Maintenance without images" : "Maintenance with images",
                "%d maintenance image rows found.".formatted(rows.size()),
                rows
        );
    }

    private List<Map<String, Object>> maintenanceRows(UUID organizationId, String search, String propertySearch, String categoryOrTypeSearch, String status, String itemSearch, int limit) {
        return query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.description,
                       mc.name AS category_name,
                       mt.name AS type_name,
                       mr.performed_at,
                       mr.scheduled_at,
                       mr.cost,
                       mr.status,
                       COUNT(DISTINCT mri.id) AS item_count,
                       COUNT(DISTINCT img.id) AS image_count
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = mr.maintenance_type_id
                LEFT JOIN maintenance_record_items mri ON mri.maintenance_record_id = mr.id
                                                     AND mri.organization_id = mr.organization_id
                LEFT JOIN maintenance_record_images img ON img.maintenance_record_id = mr.id
                                                       AND img.organization_id = mr.organization_id
                                                       AND img.deleted_at IS NULL
                                                       AND img.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND (CAST(:status AS TEXT) IS NULL OR mr.status = CAST(:status AS TEXT))
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(CONCAT_WS(' ', mr.title, mr.description, mc.name, mt.name, p.name)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', mr.title, mr.description, mc.name, mt.name, p.name)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:propertySearch AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', p.name, p.address, p.description)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:categoryOrTypeSearch AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:categoryOrTypeSearch AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', mc.name, mt.name)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:itemSearch AS TEXT) IS NULL
                       OR EXISTS (
                           SELECT 1
                           FROM maintenance_record_items item_filter
                           WHERE item_filter.maintenance_record_id = mr.id
                             AND item_filter.organization_id = mr.organization_id
                             AND NOT EXISTS (
                                 SELECT 1
                                 FROM unnest(string_to_array(CAST(:itemSearch AS TEXT), ' ')) AS token(value)
                                 WHERE token.value <> ''
                                   AND LOWER(CONCAT_WS(' ', item_filter.item_name_snapshot, item_filter.notes)) NOT LIKE CONCAT('%', token.value, '%')
                             )
                       )
                  )
                GROUP BY mr.id, p.name, mr.title, mr.description, mc.name, mt.name, mr.performed_at, mr.scheduled_at, mr.cost, mr.status
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("propertySearch", propertySearch);
                    q.setParameter("categoryOrTypeSearch", categoryOrTypeSearch);
                    q.setParameter("status", status);
                    q.setParameter("itemSearch", itemSearch);
                    q.setParameter("limit", limit);
                }, "id", "propertyName", "title", "description", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status", "itemCount", "imageCount");
    }

    private AiToolAnswer maintenanceRowsAnswer(List<Map<String, Object>> rows, String toolName, String label, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | categoría: ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("typeName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("performedAt"))));
            if (!value(row.get("cost")).isBlank()) {
                answer.append(" | costo: ").append(formatMoney(row.get("cost")));
            }
            answer.append(" | items: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d maintenance rows found.".formatted(rows.size()), rows);
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
        List<Map<String, Object>> candidates = query("""
                SELECT p.id, p.name, p.status, p.address, p.description
                FROM properties p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS TEXT))
                ORDER BY p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("status", status);
                    q.setParameter("limit", Math.max(limit, 50));
                }, "id", "name", "status", "address", "description");

        if (search == null || search.isBlank()) {
            return candidates.stream().limit(limit).toList();
        }

        return candidates.stream()
                .filter(row -> propertyMatchScore(row, search) > 0)
                .sorted((left, right) -> Integer.compare(propertyMatchScore(right, search), propertyMatchScore(left, search)))
                .limit(limit)
                .toList();
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

    private Map<String, Object> bestPropertyMatch(List<Map<String, Object>> candidates, String search) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (search == null || search.isBlank()) {
            return candidates.get(0);
        }

        Map<String, Object> best = null;
        int bestScore = 0;
        for (Map<String, Object> row : candidates) {
            int score = propertyMatchScore(row, search);
            if (score > bestScore) {
                bestScore = score;
                best = row;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private int propertyMatchScore(Map<String, Object> row, String search) {
        String propertyName = value(row.get("name"));
        String haystack = normalize(String.join(" ",
                propertyName,
                value(row.get("address")),
                value(row.get("description"))
        ));
        String normalizedSearch = normalize(search);
        if (haystack.isBlank() || normalizedSearch.isBlank()) {
            return 0;
        }
        if (haystack.contains(normalizedSearch)) {
            return 100 + normalizedSearch.length();
        }

        List<String> searchTokens = searchTokens(normalizedSearch);
        List<String> haystackTokens = searchTokens(haystack);
        if (searchTokens.isEmpty() || haystackTokens.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (String searchToken : searchTokens) {
            if (haystackTokens.stream().anyMatch(haystackToken -> tokenMatches(searchToken, haystackToken))) {
                score++;
            }
        }

        int requiredMatches = searchTokens.size() <= 2 ? searchTokens.size() : Math.max(2, searchTokens.size() - 1);
        return score >= requiredMatches ? score : 0;
    }

    private List<String> searchTokens(String value) {
        return Arrays.stream(normalize(value).split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .filter(token -> !SEARCH_STOP_WORDS.contains(token))
                .toList();
    }

    private boolean tokenMatches(String needle, String candidate) {
        if (candidate.equals(needle) || candidate.contains(needle) || needle.contains(candidate)) {
            return true;
        }
        return needle.length() >= 5 && candidate.length() >= 5 && levenshteinDistanceAtMostOne(needle, candidate);
    }

    private boolean levenshteinDistanceAtMostOne(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 1) {
            return false;
        }
        int i = 0;
        int j = 0;
        int edits = 0;
        while (i < left.length() && j < right.length()) {
            if (left.charAt(i) == right.charAt(j)) {
                i++;
                j++;
                continue;
            }
            edits++;
            if (edits > 1) {
                return false;
            }
            if (left.length() > right.length()) {
                i++;
            } else if (right.length() > left.length()) {
                j++;
            } else {
                i++;
                j++;
            }
        }
        return edits + (left.length() - i) + (right.length() - j) <= 1;
    }

    private String indentCatalogAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "- Sin datos configurados.";
        }
        return Arrays.stream(answer.split("\\R"))
                .filter(line -> line.trim().startsWith("-"))
                .map(line -> "   " + line.trim())
                .collect(Collectors.joining(System.lineSeparator()));
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
