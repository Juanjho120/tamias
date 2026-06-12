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
        String search = nullableSearch(extractSearchText(userQuestion, "historial", "historia", "mantenimiento", "mantenimientos", "programado", "programados"));
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\s+') token(value)
                          WHERE translate(LOWER(CONCAT_WS(' ', mr.title, mr.description, p.name, mc.name, mt.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append("""
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (search != null) {
                        q.setParameter("search", search);
                    }
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
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (from != null) {
            sql.append(" AND sm.next_due_date >= :fromDate\n");
        }
        if (to != null) {
            sql.append(" AND sm.next_due_date <= :toDate\n");
        }
        if (normalizedSearch != null) {
            sql.append("""
                     AND (sm.status = CAST(:search AS TEXT) OR NOT EXISTS (
                         SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                         WHERE translate(LOWER(CONCAT_WS(' ', sm.title, sm.description, p.name, mc.name, mt.name, sm.frequency, sm.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                     ))
                    """);
        }
        sql.append("""
                ORDER BY sm.next_due_date ASC, sm.title ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (from != null) {
                        q.setParameter("fromDate", Date.valueOf(from));
                    }
                    if (to != null) {
                        q.setParameter("toDate", Date.valueOf(to));
                    }
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
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
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS reservation_count,
                       COALESCE(SUM(r.reservation_value), 0) AS total_revenue,
                       COALESCE(AVG(r.reservation_value), 0) AS average_revenue,
                       COALESCE(SUM(r.check_out - r.check_in), 0) AS total_nights
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                """);
        appendOptionalReservationDateFilters(sql, range);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    setOptionalReservationDateParameters(q, range);
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
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(r.check_out - r.check_in), 0) AS total_nights,
                       COUNT(*) AS reservation_count
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                """);
        appendOptionalReservationDateFilters(sql, range);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    setOptionalReservationDateParameters(q, range);
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
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT rg.guest_id) AS unique_guests,
                       COUNT(rg.id) AS guest_reservation_links,
                       COUNT(DISTINCT r.id) AS reservation_count
                FROM reservations r
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                """);
        appendOptionalReservationDateFilters(sql, range);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    setOptionalReservationDateParameters(q, range);
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
        StringBuilder sql = new StringBuilder("""
                SELECT p.name AS property_name,
                       COUNT(r.id) AS reservation_count,
                       COALESCE(SUM(r.check_out - r.check_in), 0) AS reserved_nights
                FROM properties p
                LEFT JOIN reservations r ON r.property_id = p.id
                                       AND r.organization_id = p.organization_id
                                       AND r.deleted_at IS NULL
                                       AND r.status = 'ACTIVE'
                """);
        if (range[0] != null) {
            sql.append(" AND r.check_in >= :fromDate\n");
        }
        if (range[1] != null) {
            sql.append(" AND r.check_in <= :toDate\n");
        }
        sql.append("""
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                GROUP BY p.id, p.name
                ORDER BY reserved_nights DESC, reservation_count DESC, p.name ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    setOptionalReservationDateParameters(q, range);
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
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (currentOnly) {
            sql.append(" AND r.check_in <= :fromDate AND r.check_out > :fromDate\n");
        } else {
            if (from != null) {
                sql.append(" AND r.check_in >= :fromDate\n");
            }
            if (to != null) {
                sql.append(" AND r.check_in <= :toDate\n");
            }
        }
        if (normalizedSearch != null) {
            sql.append("""
                     AND (r.status = CAST(:search AS TEXT) OR NOT EXISTS (
                         SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                         WHERE translate(LOWER(CONCAT_WS(' ', p.name, pl.name, r.reservation_code, r.observations, r.status, g.full_name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                     ))
                    """);
        }
        sql.append("""
                GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status
                ORDER BY r.check_in ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (currentOnly || from != null) {
                        q.setParameter("fromDate", Date.valueOf(from));
                    }
                    if (!currentOnly && to != null) {
                        q.setParameter("toDate", Date.valueOf(to));
                    }
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
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
                """
                + "  AND " + whereClause + System.lineSeparator()
                + "GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status" + System.lineSeparator()
                + "ORDER BY " + orderBy + System.lineSeparator()
                + "LIMIT :limit" + System.lineSeparator();
    }

    private void appendOptionalReservationDateFilters(StringBuilder sql, LocalDate[] range) {
        if (range[0] != null) {
            sql.append(" AND r.check_in >= :fromDate\n");
        }
        if (range[1] != null) {
            sql.append(" AND r.check_in <= :toDate\n");
        }
    }

    private void setOptionalReservationDateParameters(Query query, LocalDate[] range) {
        if (range[0] != null) {
            query.setParameter("fromDate", Date.valueOf(range[0]));
        }
        if (range[1] != null) {
            query.setParameter("toDate", Date.valueOf(range[1]));
        }
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
        StringBuilder sql = new StringBuilder("""
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
                """);
        appendOptionalReservationDateFilters(sql, range);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    setOptionalReservationDateParameters(q, range);
                }, "uniqueGuests", "guestLinks");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Conteo de huéspedes:"
                + System.lineSeparator() + "- Huéspedes únicos: " + blankToDash(value(row.get("uniqueGuests")))
                + System.lineSeparator() + "- Asignaciones a reservaciones: " + blankToDash(value(row.get("guestLinks")));
        return AiToolAnswer.of(answer, "guest.countByDateRange", "Guest count by date range", "Guest count by date range was calculated.", rows);
    }

    private AiToolAnswer guestList(String toolName, String label, String intro, String search, boolean upcomingOnly, boolean recentOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (normalizedSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\s+') token(value)
                          WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, g.notes, g.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (upcomingOnly) {
            sql.append("  AND r.check_in >= CURRENT_DATE\n");
        }
        sql.append("""
                GROUP BY g.id, g.full_name, g.status
                ORDER BY MAX(r.check_in) DESC NULLS LAST, g.full_name ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
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
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
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
                """);
        if (upcomingOnly) {
            sql.append("  AND r.check_in >= CURRENT_DATE\n");
        }
        if (normalizedSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\s+') token(value)
                          WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append("""
                ORDER BY r.check_in DESC, rg.is_primary DESC, g.full_name ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
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

    public AiToolAnswer purchaseListSearch(String userQuestion) {
        PurchaseDateRange range = purchaseDateRange(userQuestion);
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "compra", "compras", "hice", "realice", "realizadas", "lista", "listas", "pendiente", "pendientes", "completada", "completadas", "mes", "semana", "ano", "year"
        ));
        List<Map<String, Object>> rows = purchaseListRows(search, null, range, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    range == null
                            ? "No encontré listas de compras que coincidan con tu pregunta."
                            : "No encontré listas de compras para " + range.label() + ".",
                    "purchaseList.search",
                    "Purchase lists",
                    "No purchase lists found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(range == null ? "Estas son las listas de compras que encontré:" : "Estas son las compras que encontré para " + range.label() + ":");
        appendPurchaseListRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseList.search", "Purchase lists", "%d purchase lists found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseListsByProperty(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "compras", "compra", "propiedad", "casa", "bungalow", "alojamiento", "de", "por"));
        List<Map<String, Object>> rows = purchaseListRows(search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré listas de compras asociadas a propiedades." : "No encontré listas de compras relacionadas con “" + search + "”.",
                    "purchaseList.byProperty",
                    "Purchase lists by property",
                    "No purchase lists found by property.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(search == null ? "Estas compras están asociadas a propiedades:" : "Estas compras están relacionadas con “" + search + "”:");
        appendPurchaseListRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseList.byProperty", "Purchase lists by property", "%d purchase lists found by property.".formatted(rows.size()), rows);
    }

    public AiToolAnswer recentPurchaseLists() {
        List<Map<String, Object>> rows = purchaseListRows(null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de compras recientes.", "purchaseList.recent", "Recent purchase lists", "No recent purchase lists found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas son tus listas de compras más recientes:");
        appendPurchaseListRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseList.recent", "Recent purchase lists", "%d recent purchase lists found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer pendingPurchaseLists() {
        List<Map<String, Object>> rows = purchaseListRows(null, List.of("OPEN", "PARTIALLY_PURCHASED"), null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de compras pendientes.", "purchaseList.pending", "Pending purchase lists", "No pending purchase lists found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas listas de compras siguen pendientes o parcialmente compradas:");
        appendPurchaseListRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseList.pending", "Pending purchase lists", "%d pending purchase lists found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer completedPurchaseLists() {
        List<Map<String, Object>> rows = purchaseListRows(null, List.of("COMPLETED"), null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de compras completadas.", "purchaseList.completed", "Completed purchase lists", "No completed purchase lists found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas listas de compras están completadas:");
        appendPurchaseListRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseList.completed", "Completed purchase lists", "%d completed purchase lists found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseCostSummary(String userQuestion) {
        PurchaseDateRange range = purchaseDateRange(userQuestion);
        boolean supplyOnly = containsAny(normalize(userQuestion), "supply", "supplies", "suministro", "suministros");
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT pl.id) AS list_count,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(AVG(pi.estimated_price), 0) AS avg_line_cost,
                       MIN(pl.purchase_date) AS first_purchase_date,
                       MAX(pl.purchase_date) AS last_purchase_date
                FROM purchase_lists pl
                JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                      AND pi.organization_id = pl.organization_id
                                      AND pi.purchased = TRUE
                """);
        if (supplyOnly) {
            sql.append("""
                JOIN inventory_items ii ON ii.id = pi.inventory_item_id
                                       AND ii.organization_id = pi.organization_id
                                       AND ii.item_type = 'SUPPLY'
                """);
        }
        sql.append("""
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
            setPurchaseCostCommonParams(q, range);
        }, "listCount", "purchasedItemCount", "totalQuantity", "totalCost", "avgLineCost", "firstPurchaseDate", "lastPurchaseDate");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String scope = supplyOnly ? " de supplies" : "";
        String label = range == null ? "en tus compras" + scope : "en " + range.label() + scope;
        String answer = "Resumen de gastos " + label + ":\n"
                + "- Listas consideradas: " + blankToDash(value(row.get("listCount"))) + "\n"
                + "- Items marcados como comprados: " + blankToDash(value(row.get("purchasedItemCount"))) + "\n"
                + "- Cantidad total comprada: " + blankToDash(value(row.get("totalQuantity"))) + "\n"
                + "- Gasto estimado total: " + formatMoney(row.get("totalCost")) + "\n"
                + "- Costo promedio por línea: " + formatMoney(row.get("avgLineCost"));
        return AiToolAnswer.of(answer, "purchaseList.costSummary", "Purchase cost summary", "Purchase cost summary was calculated.", rows);
    }

    public AiToolAnswer purchaseCostByProperty() {
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(p.name, 'Sin propiedad') AS property_name,
                       COUNT(DISTINCT pl.id) AS list_count,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost
                FROM purchase_lists pl
                JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                      AND pi.organization_id = pl.organization_id
                                      AND pi.purchased = TRUE
                LEFT JOIN properties p ON p.id = pl.property_id
                                      AND p.organization_id = pl.organization_id
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                GROUP BY COALESCE(p.name, 'Sin propiedad')
                ORDER BY total_cost DESC, purchased_item_count DESC, property_name ASC
                LIMIT :limit
                """, q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "propertyName", "listCount", "purchasedItemCount", "totalQuantity", "totalCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para calcular gasto por propiedad.", "purchaseList.costByProperty", "Purchase cost by property", "No purchase cost rows by property found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Gasto estimado de compras por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | gasto: ").append(formatMoney(row.get("totalCost")))
                    .append(" | items comprados: ").append(blankToDash(value(row.get("purchasedItemCount"))))
                    .append(" | listas: ").append(blankToDash(value(row.get("listCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseList.costByProperty", "Purchase cost by property", "%d purchase cost rows by property found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseCostByCategory() {
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(ii.item_type, 'SNAPSHOT_ONLY') AS category,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(STRING_AGG(DISTINCT pi.item_name_snapshot, ', ' ORDER BY pi.item_name_snapshot), '') AS sample_items
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                                      AND pl.organization_id = pi.organization_id
                LEFT JOIN inventory_items ii ON ii.id = pi.inventory_item_id
                                            AND ii.organization_id = pi.organization_id
                WHERE pi.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                  AND pi.purchased = TRUE
                GROUP BY COALESCE(ii.item_type, 'SNAPSHOT_ONLY')
                ORDER BY total_cost DESC, purchased_item_count DESC, category ASC
                LIMIT :limit
                """, q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "category", "purchasedItemCount", "totalQuantity", "totalCost", "sampleItems");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para calcular gasto por categoría.", "purchaseList.costByCategory", "Purchase cost by category", "No purchase cost rows by category found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Gasto estimado de compras por categoría/tipo de item:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("category"))))
                    .append(" | gasto: ").append(formatMoney(row.get("totalCost")))
                    .append(" | items comprados: ").append(blankToDash(value(row.get("purchasedItemCount"))))
                    .append(" | ejemplos: ").append(blankToDash(value(row.get("sampleItems"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseList.costByCategory", "Purchase cost by category", "%d purchase cost rows by category found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseCostByMonth() {
        List<Map<String, Object>> rows = query("""
                SELECT TO_CHAR(DATE_TRUNC('month', pl.purchase_date), 'YYYY-MM') AS purchase_month,
                       COUNT(DISTINCT pl.id) AS list_count,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost
                FROM purchase_lists pl
                JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                      AND pi.organization_id = pl.organization_id
                                      AND pi.purchased = TRUE
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                GROUP BY DATE_TRUNC('month', pl.purchase_date)
                ORDER BY DATE_TRUNC('month', pl.purchase_date) DESC
                LIMIT :limit
                """, q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", 12);
        }, "purchaseMonth", "listCount", "purchasedItemCount", "totalQuantity", "totalCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para calcular gasto por mes.", "purchaseList.costByMonth", "Purchase cost by month", "No purchase cost rows by month found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Gasto estimado de compras por mes:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("purchaseMonth"))))
                    .append(" | gasto: ").append(formatMoney(row.get("totalCost")))
                    .append(" | items comprados: ").append(blankToDash(value(row.get("purchasedItemCount"))))
                    .append(" | listas: ").append(blankToDash(value(row.get("listCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseList.costByMonth", "Purchase cost by month", "%d purchase cost rows by month found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemSearch(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "item", "items", "producto", "productos", "compra", "compras", "comprado", "comprados", "buscar", "busca"));
        List<Map<String, Object>> rows = purchaseItemRows(search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré items de compras." : "No encontré items de compras relacionados con “" + search + "”.", "purchaseItem.search", "Purchase items", "No purchase items found.", List.of());
        }
        StringBuilder answer = new StringBuilder(search == null ? "Estos son los items de compras que encontré:" : "Estos items de compras coinciden con “" + search + "”:");
        appendPurchaseItemRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseItem.search", "Purchase items", "%d purchase items found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemsByPurchaseList(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "items", "item", "lista", "compra", "compras", "de", "la"));
        List<Map<String, Object>> rows = purchaseItemRows(search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré items asociados a listas de compras." : "No encontré items asociados a una lista de compras relacionada con “" + search + "”.", "purchaseItem.byPurchaseList", "Purchase items by purchase list", "No purchase items found by list.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos items aparecen en listas de compras:");
        appendPurchaseItemRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseItem.byPurchaseList", "Purchase items by purchase list", "%d purchase items found by list.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemsByInventoryItem(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "inventario", "inventory", "item", "items", "producto", "productos", "compras", "compra"));
        List<Map<String, Object>> rows = purchaseItemRows(search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré compras vinculadas a items de inventario." : "No encontré compras vinculadas al item “" + search + "”.", "purchaseItem.byInventoryItem", "Purchase items by inventory item", "No purchase items found by inventory item.", List.of());
        }
        StringBuilder answer = new StringBuilder(search == null ? "Estas compras están vinculadas a items de inventario:" : "Estas compras están vinculadas a “" + search + "”:");
        appendPurchaseItemRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "purchaseItem.byInventoryItem", "Purchase items by inventory item", "%d purchase items found by inventory item.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemPriceHistory(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "precio", "precios", "historial", "cuesta", "normalmente", "costo", "costos", "compra", "compras", "item", "producto"));
        if (search == null) {
            return AiToolAnswer.of("Dime el nombre del producto para revisar su historial de precios. Por ejemplo: “¿Cuánto cuesta normalmente el papel higiénico?”.", "purchaseItem.priceHistory", "Purchase item price history", "No item name provided for price history.", List.of());
        }
        List<Map<String, Object>> rows = purchaseItemRows(search, List.of("purchasedOnly"), null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para “" + search + "”.", "purchaseItem.priceHistory", "Purchase item price history", "No purchased items found for price history.", List.of());
        }
        StringBuilder answer = new StringBuilder("Historial de precios encontrado para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("purchaseDate"))))
                    .append(" | ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | precio: ").append(formatMoney(row.get("estimatedPrice")))
                    .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseItem.priceHistory", "Purchase item price history", "%d purchase price history rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemAverageUnitCost(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "cuanto", "cuesta", "normalmente", "promedio", "precio", "costo", "unitario", "compra", "compras", "item", "producto", "productos"));
        List<Map<String, Object>> rows = query(purchaseItemAggregateSql(search, "item_name", "DESC"), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalCost", "averageLineCost", "averageUnitCost", "firstPurchaseDate", "lastPurchaseDate");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré compras para calcular costos promedio." : "No encontré compras para calcular el costo promedio de “" + search + "”.", "purchaseItem.averageUnitCost", "Average unit cost", "No average unit cost rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(search == null ? "Estos son los costos promedio de items comprados:" : "Costo promedio encontrado para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | costo unitario promedio: ").append(formatMoney(row.get("averageUnitCost")))
                    .append(" | costo promedio por línea: ").append(formatMoney(row.get("averageLineCost")))
                    .append(" | compras: ").append(blankToDash(value(row.get("purchaseCount"))))
                    .append(" | última compra: ").append(blankToDash(value(row.get("lastPurchaseDate"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseItem.averageUnitCost", "Average unit cost", "%d average unit cost rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemQuantitySummary(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "cantidad", "cantidades", "cuanto", "cuantos", "compre", "comprado", "comprados", "item", "items", "producto", "productos"));
        List<Map<String, Object>> rows = query(purchaseItemAggregateSql(search, "total_quantity", "DESC"), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalCost", "averageLineCost", "averageUnitCost", "firstPurchaseDate", "lastPurchaseDate");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré compras para resumir cantidades." : "No encontré compras para resumir cantidades de “" + search + "”.", "purchaseItem.quantitySummary", "Purchase item quantity summary", "No quantity summary rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(search == null ? "Resumen de cantidades compradas por item:" : "Resumen de cantidades para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | compras: ").append(blankToDash(value(row.get("purchaseCount"))))
                    .append(" | gasto: ").append(formatMoney(row.get("totalCost")));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseItem.quantitySummary", "Purchase item quantity summary", "%d quantity summary rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer purchaseItemMostPurchased() {
        List<Map<String, Object>> rows = query(purchaseItemAggregateSql(null, "total_quantity", "DESC"), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", 1);
        }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalCost", "averageLineCost", "averageUnitCost", "firstPurchaseDate", "lastPurchaseDate");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para identificar el item que compras más seguido.", "purchaseItem.mostPurchased", "Most purchased item", "No most purchased row found.", List.of());
        }
        Map<String, Object> row = rows.getFirst();
        String answer = "El item que compras más seguido es " + blankToDash(value(row.get("itemName")))
                + ", con cantidad total comprada de " + blankToDash(value(row.get("totalQuantity"))) + " " + blankToDash(value(row.get("unit")))
                + " en " + blankToDash(value(row.get("purchaseCount"))) + " compra(s) registradas"
                + " y gasto total " + formatMoney(row.get("totalCost")) + ".";
        return AiToolAnswer.of(answer, "purchaseItem.mostPurchased", "Most purchased item", "1 most purchased row found.", rows);
    }

    public AiToolAnswer purchaseItemLeastPurchased() {
        List<Map<String, Object>> rows = query(purchaseItemAggregateSql(null, "total_quantity", "ASC"), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", 1);
        }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalCost", "averageLineCost", "averageUnitCost", "firstPurchaseDate", "lastPurchaseDate");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré compras marcadas como compradas para identificar el item que compras menos seguido.", "purchaseItem.leastPurchased", "Least purchased item", "No least purchased row found.", List.of());
        }
        Map<String, Object> row = rows.getFirst();
        String answer = "El item que compras menos seguido es " + blankToDash(value(row.get("itemName")))
                + ", con cantidad total comprada de " + blankToDash(value(row.get("totalQuantity"))) + " " + blankToDash(value(row.get("unit")))
                + " en " + blankToDash(value(row.get("purchaseCount"))) + " compra(s) registradas"
                + " y gasto total " + formatMoney(row.get("totalCost")) + ".";
        return AiToolAnswer.of(answer, "purchaseItem.leastPurchased", "Least purchased item", "1 least purchased row found.", rows);
    }

    public AiToolAnswer purchaseItemCostTrend(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "ha", "subido", "bajado", "precio", "precios", "producto", "productos", "item", "items", "costo", "costos", "alguno", "algun"));
        String sql = """
                WITH priced_items AS (
                    SELECT pi.item_name_snapshot,
                           pl.purchase_date,
                           CASE WHEN pi.quantity IS NOT NULL AND pi.quantity > 0
                                THEN pi.estimated_price / pi.quantity
                                ELSE pi.estimated_price
                           END AS unit_price,
                           LAG(CASE WHEN pi.quantity IS NOT NULL AND pi.quantity > 0
                                    THEN pi.estimated_price / pi.quantity
                                    ELSE pi.estimated_price
                               END) OVER (PARTITION BY translate(LOWER(pi.item_name_snapshot), 'áéíóúüñ', 'aeiouun') ORDER BY pl.purchase_date, pi.created_at) AS previous_unit_price
                    FROM purchase_items pi
                    JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                                          AND pl.organization_id = pi.organization_id
                    WHERE pi.organization_id = :organizationId
                      AND pl.deleted_at IS NULL
                      AND pi.purchased = TRUE
                      AND pi.estimated_price IS NOT NULL
                """;
        if (search != null) {
            sql += """
                      AND NOT EXISTS (
                          SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """;
        }
        sql += """
                ), latest_changes AS (
                    SELECT item_name_snapshot,
                           purchase_date,
                           unit_price,
                           previous_unit_price,
                           unit_price - previous_unit_price AS price_change,
                           ROW_NUMBER() OVER (PARTITION BY translate(LOWER(item_name_snapshot), 'áéíóúüñ', 'aeiouun') ORDER BY purchase_date DESC) AS rn
                    FROM priced_items
                    WHERE previous_unit_price IS NOT NULL
                )
                SELECT item_name_snapshot,
                       purchase_date,
                       previous_unit_price,
                       unit_price,
                       price_change
                FROM latest_changes
                WHERE rn = 1
                ORDER BY price_change DESC, purchase_date DESC, item_name_snapshot ASC
                LIMIT :limit
                """;
        List<Map<String, Object>> rows = query(sql, q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "purchaseDate", "previousUnitPrice", "unitPrice", "priceChange");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré suficientes compras repetidas con precio para calcular tendencias." : "No encontré suficientes compras repetidas de “" + search + "” para calcular tendencia de precio.", "purchaseItem.costTrend", "Purchase item cost trend", "No cost trend rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(search == null ? "Estas son las últimas variaciones de precio que encontré:" : "Tendencia de precio encontrada para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | anterior: ").append(formatMoney(row.get("previousUnitPrice")))
                    .append(" | último: ").append(formatMoney(row.get("unitPrice")))
                    .append(" | cambio: ").append(formatMoney(row.get("priceChange")))
                    .append(" | fecha: ").append(blankToDash(value(row.get("purchaseDate"))));
        }
        return AiToolAnswer.of(answer.toString(), "purchaseItem.costTrend", "Purchase item cost trend", "%d cost trend rows found.".formatted(rows.size()), rows);
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


    public AiToolAnswer reservationSupplySearch(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "supply", "supplies", "insumo", "insumos", "suministro", "suministros",
                "reservacion", "reservaciones", "reserva", "reservas", "asignado", "asignados", "usado", "usados",
                "necesito", "necesarios", "necesarias", "proxima", "proximas", "proximo", "proximos", "ultima", "ultimo", "mas", "más", "tienen"
        ));
        List<Map<String, Object>> rows = reservationSupplyRows(search, null, null, null, null, null, DEFAULT_LIMIT, "r.check_in DESC NULLS LAST, rs.item_name_snapshot ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré supplies registrados en reservaciones." : "No encontré supplies de reservación relacionados con “" + search + "”.",
                    "reservationSupply.search",
                    "Reservation supply search",
                    "No reservation supplies found.",
                    List.of()
            );
        }
        return reservationSupplyRowsAnswer(rows, "reservationSupply.search", search == null ? "Estos supplies están registrados en reservaciones:" : "Encontré estos supplies de reservación relacionados con “" + search + "”:");
    }

    public AiToolAnswer reservationSuppliesByReservation(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "supply", "supplies", "insumo", "insumos", "suministro", "suministros",
                "reservacion", "reservaciones", "reserva", "reservas", "codigo", "para", "asignado", "asignados"
        ));
        List<Map<String, Object>> rows = reservationSupplyRows(null, null, null, null, search, null, DEFAULT_LIMIT, "r.check_in DESC NULLS LAST, rs.item_name_snapshot ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré supplies asociados a reservaciones." : "No encontré supplies para una reservación relacionada con “" + search + "”.",
                    "reservationSupply.byReservation",
                    "Reservation supplies by reservation",
                    "No reservation supplies found for reservation filter.",
                    List.of()
            );
        }
        return reservationSupplyRowsAnswer(rows, "reservationSupply.byReservation", search == null ? "Estos supplies están asociados a reservaciones:" : "Estos supplies están asociados a reservaciones relacionadas con “" + search + "”:");
    }

    public AiToolAnswer reservationSuppliesByProperty(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "supply", "supplies", "insumo", "insumos", "suministro", "suministros",
                "propiedad", "propiedades", "casa", "bungalow", "reservacion", "reservaciones", "reserva", "reservas"
        ));
        List<Map<String, Object>> rows = reservationSupplyRows(null, null, null, search, null, null, DEFAULT_LIMIT, "p.name ASC, r.check_in DESC NULLS LAST, rs.item_name_snapshot ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré supplies por propiedad." : "No encontré supplies para una propiedad relacionada con “" + search + "”.",
                    "reservationSupply.byProperty",
                    "Reservation supplies by property",
                    "No reservation supplies found for property filter.",
                    List.of()
            );
        }
        return reservationSupplyRowsAnswer(rows, "reservationSupply.byProperty", search == null ? "Estos supplies están asociados por propiedad:" : "Estos supplies están asociados a propiedades relacionadas con “" + search + "”:");
    }

    public AiToolAnswer reservationSuppliesForUpcomingReservations() {
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(14);
        List<Map<String, Object>> rows = reservationSupplyRows(null, today, toDate, null, null, null, DEFAULT_LIMIT, "r.check_in ASC, p.name ASC, rs.item_name_snapshot ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré supplies asignados para reservaciones activas con check-in en los próximos 14 días.",
                    "reservationSupply.forUpcomingReservations",
                    "Reservation supplies for upcoming reservations",
                    "No supplies found for upcoming reservations.",
                    List.of()
            );
        }
        return reservationSupplyRowsAnswer(rows, "reservationSupply.forUpcomingReservations", "Estos supplies están asignados a próximas reservaciones:");
    }

    public AiToolAnswer reservationSuppliesForLatestPastReservation() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> reservations = query("""
                SELECT r.id,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in <= :today
                ORDER BY r.check_in DESC, r.created_at DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }, "id", "propertyName", "reservationCode", "checkIn", "checkOut");
        if (reservations.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré una reservación activa pasada o de hoy para revisar supplies usados.",
                    "reservationSupply.byReservation",
                    "Reservation supplies for latest past reservation",
                    "No past reservation found for supply lookup.",
                    List.of()
            );
        }
        Map<String, Object> reservation = reservations.get(0);
        UUID reservationId = UUID.fromString(value(reservation.get("id")));
        List<Map<String, Object>> rows = query("""
                SELECT rs.id,
                       rs.item_name_snapshot AS item_name,
                       rs.quantity,
                       COALESCE(rs.unit, '') AS unit,
                       COALESCE(rs.notes, '') AS notes,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.status
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE rs.organization_id = :organizationId
                  AND rs.reservation_id = :reservationId
                  AND r.deleted_at IS NULL
                ORDER BY rs.item_name_snapshot ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("reservationId", reservationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "itemName", "quantity", "unit", "notes", "propertyName", "reservationCode", "checkIn", "checkOut", "status");
        String reservationLabel = blankToDash(value(reservation.get("reservationCode")));
        String propertyName = blankToDash(value(reservation.get("propertyName")));
        String checkIn = blankToDash(value(reservation.get("checkIn")));
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré supplies asociados a la última reservación " + reservationLabel + " de " + propertyName + " con check-in " + checkIn + ".",
                    "reservationSupply.byReservation",
                    "Reservation supplies for latest past reservation",
                    "Latest past reservation found, but no supplies were assigned.",
                    reservations
            );
        }
        StringBuilder answer = new StringBuilder("Estos supplies se usaron en la última reservación que encontré: ")
                .append(reservationLabel)
                .append(" | ").append(propertyName)
                .append(" | check-in: ").append(checkIn)
                .append(".");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity"))))
                    .append(" ").append(blankToDash(value(row.get("unit"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.byReservation", "Reservation supplies for latest past reservation", "%d supplies found for latest past reservation.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationSupplySummaryByItem(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        List<Map<String, Object>> rows = reservationSupplySummaryRows(range[0], range[1], DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré supplies registrados en reservaciones para resumir por item.",
                    "reservationSupply.summaryByItem",
                    "Reservation supply summary by item",
                    "No reservation supply item summary found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estos son los supplies resumidos por item en reservaciones:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | registros: ").append(blankToDash(value(row.get("usageCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | última reserva: ").append(blankToDash(value(row.get("lastCheckIn"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.summaryByItem", "Reservation supply summary by item", "%d reservation supply item summary rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationSupplySummaryByDateRange(String userQuestion) {
        LocalDate[] range = resolveDateRange(userQuestion);
        List<Map<String, Object>> rows = query("""
                SELECT r.check_in AS check_in,
                       COUNT(rs.id) AS supply_count,
                       COALESCE(SUM(rs.quantity), 0) AS total_quantity,
                       COALESCE(STRING_AGG(DISTINCT rs.item_name_snapshot, ', ' ORDER BY rs.item_name_snapshot), '') AS items
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                """ + dateFilterSql("r.check_in", range[0], range[1]) + """
                GROUP BY r.check_in
                ORDER BY r.check_in ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    setDateRangeParams(q, range[0], range[1]);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "checkIn", "supplyCount", "totalQuantity", "items");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré supplies de reservación en el rango consultado.", "reservationSupply.summaryByDateRange", "Reservation supply summary by date range", "No reservation supply date summary found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Este es el resumen de supplies por fecha de check-in:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" | registros: ").append(blankToDash(value(row.get("supplyCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity"))))
                    .append(" | items: ").append(blankToDash(value(row.get("items"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.summaryByDateRange", "Reservation supply summary by date range", "%d reservation supply date summary rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationSupplyLastUsed(String userQuestion) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "ultimo", "ultima", "ultimos", "ultimas", "vez", "cuando", "uso", "usado", "usaron", "se", "supply", "supplies", "insumo", "insumos", "reservacion", "reservaciones", "reserva", "reservas"
        ));
        List<Map<String, Object>> rows = reservationSupplyRows(search, null, null, null, null, null, 1, "r.check_in DESC NULLS LAST, r.created_at DESC, rs.item_name_snapshot ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré supplies usados en reservaciones." : "No encontré uso de supplies en reservaciones para “" + search + "”.",
                    "reservationSupply.lastUsed",
                    "Reservation supply last used",
                    "No last reservation supply usage found.",
                    List.of()
            );
        }
        Map<String, Object> row = rows.get(0);
        String answer = "La última vez que encontré “" + blankToDash(value(row.get("itemName"))) + "” en una reservación fue para el check-in " + blankToDash(value(row.get("checkIn"))) + ".\n"
                + "Propiedad: " + blankToDash(value(row.get("propertyName"))) + ".\n"
                + "Reservación: " + blankToDash(value(row.get("reservationCode"))) + ".\n"
                + "Cantidad: " + blankToDash(value(row.get("quantity"))) + " " + blankToDash(value(row.get("unit"))) + ".";
        return AiToolAnswer.of(answer, "reservationSupply.lastUsed", "Reservation supply last used", "Last reservation supply usage found.", rows);
    }

    public AiToolAnswer reservationSupplyMostUsed() {
        List<Map<String, Object>> rows = reservationSupplySummaryRows(null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré supplies registrados en reservaciones.", "reservationSupply.mostUsed", "Most used reservation supplies", "No reservation supply usage found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son los supplies más usados en reservaciones:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | registros: ").append(blankToDash(value(row.get("usageCount"))))
                    .append(" | cantidad total: ").append(blankToDash(value(row.get("totalQuantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | propiedades: ").append(blankToDash(value(row.get("properties"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.mostUsed", "Most used reservation supplies", "%d most used reservation supplies found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationSupplyMissingForUpcomingReservations() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate toDate = today.plusDays(14);
        List<Map<String, Object>> rows = query("""
                SELECT r.id,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in BETWEEN :today AND :toDate
                  AND NOT EXISTS (
                      SELECT 1
                      FROM reservation_supplies rs
                      WHERE rs.reservation_id = r.id
                        AND rs.organization_id = r.organization_id
                  )
                GROUP BY r.id, p.name, r.reservation_code, r.check_in, r.check_out
                ORDER BY r.check_in ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("toDate", Date.valueOf(toDate));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "reservationCode", "checkIn", "checkOut", "guests");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré próximas reservaciones activas sin supplies asignados en los próximos 14 días.", "reservationSupply.missingForUpcomingReservations", "Reservations missing supplies", "No upcoming reservations missing supplies found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas próximas reservaciones no tienen supplies asignados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | código: ").append(blankToDash(value(row.get("reservationCode"))))
                    .append(" | check-in: ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" | check-out: ").append(blankToDash(value(row.get("checkOut"))))
                    .append(" | huéspedes: ").append(blankToDash(value(row.get("guests"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.missingForUpcomingReservations", "Reservations missing supplies", "%d upcoming reservations without supplies found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer taskListSearch(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "tarea", "tareas", "lista", "listas", "task", "tasks", "checklist"));
        List<Map<String, Object>> rows = taskListRows(search, null, null, null, null, null, null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, tl.creation_date DESC, tl.title ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré listas de tareas." : "No encontré listas de tareas relacionadas con “" + search + "”.", "taskList.search", "Task list search", "No task lists found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.search", search == null ? "Estas son las listas de tareas que encontré:" : "Encontré estas listas de tareas relacionadas con “" + search + "”:");
    }

    public AiToolAnswer taskListsByProperty(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "tarea", "tareas", "lista", "listas", "propiedad", "propiedades", "casa", "bungalow"));
        List<Map<String, Object>> rows = taskListRows(null, search, null, null, null, null, null, DEFAULT_LIMIT, "p.name ASC, tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré tareas asociadas a propiedades." : "No encontré tareas para propiedades relacionadas con “" + search + "”.", "taskList.byProperty", "Task lists by property", "No task lists by property found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.byProperty", search == null ? "Estas tareas están asociadas por propiedad:" : "Estas tareas están asociadas a propiedades relacionadas con “" + search + "”:");
    }

    public AiToolAnswer taskListsByReservation(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "tarea", "tareas", "lista", "listas", "reservacion", "reservaciones", "reserva", "reservas", "proxima", "proximas"));
        LocalDate fromDate = containsAny(normalize(userQuestion), "proxima", "proximas", "siguiente") ? LocalDate.now() : null;
        List<Map<String, Object>> rows = taskListRows(null, null, search, fromDate, null, null, null, DEFAULT_LIMIT, "r.check_in ASC NULLS LAST, tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré tareas asociadas a reservaciones." : "No encontré tareas para reservaciones relacionadas con “" + search + "”.", "taskList.byReservation", "Task lists by reservation", "No task lists by reservation found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.byReservation", search == null ? "Estas tareas están asociadas a reservaciones:" : "Estas tareas están asociadas a reservaciones relacionadas con “" + search + "”:");
    }

    public AiToolAnswer taskListsForNextReservation() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> reservations = query("""
                SELECT r.id,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in >= :today
                ORDER BY r.check_in ASC, r.created_at ASC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }, "id", "propertyName", "reservationCode", "checkIn", "checkOut");
        if (reservations.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré una próxima reservación activa para revisar tareas asociadas.",
                    "taskList.byReservation",
                    "Task lists for next reservation",
                    "No upcoming reservation found for task lookup.",
                    List.of()
            );
        }
        Map<String, Object> reservation = reservations.get(0);
        UUID reservationId = UUID.fromString(value(reservation.get("id")));
        List<Map<String, Object>> rows = query("""
                SELECT tl.id,
                       p.name AS property_name,
                       COALESCE(r.reservation_code, '') AS reservation_code,
                       r.check_in,
                       tl.title,
                       tl.creation_date,
                       tl.due_date,
                       tl.status,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items,
                       COALESCE(SUM(CASE WHEN ti.completed = FALSE THEN 1 ELSE 0 END), 0) AS pending_items
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = tl.organization_id AND r.deleted_at IS NULL
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.reservation_id = :reservationId
                GROUP BY tl.id, p.name, r.reservation_code, r.check_in, tl.title, tl.creation_date, tl.due_date, tl.status
                ORDER BY tl.due_date ASC NULLS LAST, tl.creation_date DESC, tl.title ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("reservationId", reservationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "reservationCode", "checkIn", "title", "creationDate", "dueDate", "status", "totalItems", "completedItems", "pendingItems");
        String reservationLabel = blankToDash(value(reservation.get("reservationCode")));
        String propertyName = blankToDash(value(reservation.get("propertyName")));
        String checkIn = blankToDash(value(reservation.get("checkIn")));
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré tareas asociadas a la próxima reservación " + reservationLabel + " de " + propertyName + " con check-in " + checkIn + ".",
                    "taskList.byReservation",
                    "Task lists for next reservation",
                    "Next reservation found, but no task lists were assigned.",
                    reservations
            );
        }
        return taskListRowsAnswer(rows, "taskList.byReservation", "Estas tareas están asociadas a la próxima reservación " + reservationLabel + " de " + propertyName + " con check-in " + checkIn + ":");
    }

    public AiToolAnswer activeTaskLists() {
        List<Map<String, Object>> rows = taskListRows(null, null, null, null, null, List.of("OPEN", "IN_PROGRESS"), null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas activas.", "taskList.active", "Active task lists", "No active task lists found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.active", "Estas son tus listas de tareas activas:");
    }

    public AiToolAnswer completedTaskLists() {
        List<Map<String, Object>> rows = taskListRows(null, null, null, null, null, List.of("COMPLETED"), null, DEFAULT_LIMIT, "tl.due_date DESC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas completadas.", "taskList.completed", "Completed task lists", "No completed task lists found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.completed", "Estas son tus listas de tareas completadas:");
    }

    public AiToolAnswer overdueTaskLists() {
        List<Map<String, Object>> rows = taskListRows(null, null, null, null, LocalDate.now().minusDays(1), List.of("OPEN", "IN_PROGRESS"), null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas vencidas.", "taskList.overdue", "Overdue task lists", "No overdue task lists found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.overdue", "Estas listas de tareas están vencidas:");
    }

    public AiToolAnswer dueTodayTaskLists() {
        List<Map<String, Object>> rows = taskListRows(null, null, null, null, null, List.of("OPEN", "IN_PROGRESS"), LocalDate.now(), DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas que venzan hoy.", "taskList.dueToday", "Task lists due today", "No task lists due today found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.dueToday", "Estas listas de tareas vencen hoy:");
    }

    public AiToolAnswer dueThisWeekTaskLists() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = taskListRows(null, null, null, today, today.plusDays(7), List.of("OPEN", "IN_PROGRESS"), null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, tl.creation_date DESC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas que venzan esta semana.", "taskList.dueThisWeek", "Task lists due this week", "No task lists due this week found.", List.of());
        }
        return taskListRowsAnswer(rows, "taskList.dueThisWeek", "Estas listas de tareas vencen esta semana:");
    }

    public AiToolAnswer taskListProgressSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT tl.id) AS task_list_count,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items,
                       COALESCE(SUM(CASE WHEN ti.completed = FALSE THEN 1 ELSE 0 END), 0) AS pending_items
                FROM task_lists tl
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                """, q -> q.setParameter("organizationId", organizationId), "taskListCount", "totalItems", "completedItems", "pendingItems");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String total = blankToDash(value(row.get("totalItems")));
        String completed = blankToDash(value(row.get("completedItems")));
        String pending = blankToDash(value(row.get("pendingItems")));
        String answer = "Resumen de avance de tareas activas:\n"
                + "- Listas activas: " + blankToDash(value(row.get("taskListCount"))) + "\n"
                + "- Items completados: " + completed + "/" + total + "\n"
                + "- Items pendientes: " + pending + ".";
        return AiToolAnswer.of(answer, "taskList.progressSummary", "Task list progress summary", "Task progress summary was calculated.", rows);
    }

    public AiToolAnswer taskListCompletionSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT tl.status,
                       COUNT(*) AS task_list_count
                FROM task_lists tl
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                GROUP BY tl.status
                ORDER BY tl.status ASC
                """, q -> q.setParameter("organizationId", organizationId), "status", "taskListCount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré listas de tareas para calcular completitud.", "taskList.completionSummary", "Task list completion summary", "No task lists found for completion summary.", List.of());
        }
        StringBuilder answer = new StringBuilder("Así está la distribución de listas de tareas por estado:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator()).append("- ").append(blankToDash(value(row.get("status")))).append(": ").append(blankToDash(value(row.get("taskListCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "taskList.completionSummary", "Task list completion summary", "%d task list status rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer taskItemSearch(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "tarea", "tareas", "item", "items", "especifica", "especificas", "detalle", "detalles"));
        List<Map<String, Object>> rows = taskItemRows(search, null, null, null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré items de tareas." : "No encontré items de tareas relacionados con “" + search + "”.", "taskItem.search", "Task item search", "No task items found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.search", search == null ? "Estos son los items de tareas que encontré:" : "Encontré estos items de tareas relacionados con “" + search + "”:");
    }

    public AiToolAnswer taskItemsByTaskList(String userQuestion) {
        String search = nullableSearch(extractSearchText(userQuestion, "tarea", "tareas", "lista", "listas", "item", "items", "checklist"));
        List<Map<String, Object>> rows = taskItemRows(null, search, null, null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(search == null ? "No encontré items asociados a listas de tareas." : "No encontré items para listas de tareas relacionadas con “" + search + "”.", "taskItem.byTaskList", "Task items by task list", "No task items by task list found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.byTaskList", search == null ? "Estos items están asociados a listas de tareas:" : "Estos items están asociados a listas relacionadas con “" + search + "”:");
    }

    public AiToolAnswer pendingTaskItems() {
        List<Map<String, Object>> rows = taskItemRows(null, null, false, null, DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas pendientes.", "taskItem.pending", "Pending task items", "No pending task items found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.pending", "Estas tareas específicas siguen pendientes:");
    }

    public AiToolAnswer completedTaskItems() {
        List<Map<String, Object>> rows = taskItemRows(null, null, true, null, DEFAULT_LIMIT, "ti.completion_date DESC NULLS LAST, ti.updated_at DESC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas completadas.", "taskItem.completed", "Completed task items", "No completed task items found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.completed", "Estas tareas específicas ya están completadas:");
    }

    public AiToolAnswer overdueTaskItems() {
        List<Map<String, Object>> rows = taskItemRows(null, null, false, LocalDate.now().minusDays(1), DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas atrasadas.", "taskItem.overdue", "Overdue task items", "No overdue task items found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.overdue", "Estas tareas específicas están atrasadas:");
    }

    public AiToolAnswer taskItemAssignedSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(NULLIF(ti.responsible_person, ''), 'Sin responsable') AS responsible_person,
                       COUNT(*) AS item_count,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items,
                       COALESCE(SUM(CASE WHEN ti.completed = FALSE THEN 1 ELSE 0 END), 0) AS pending_items,
                       COALESCE(STRING_AGG(ti.task_name, ', ' ORDER BY ti.completed ASC, ti.sort_order ASC, ti.task_name ASC), '') AS task_names
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                WHERE ti.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                GROUP BY COALESCE(NULLIF(ti.responsible_person, ''), 'Sin responsable')
                ORDER BY item_count DESC, responsible_person ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "responsiblePerson", "itemCount", "completedItems", "pendingItems", "taskNames");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré responsables asignados en tareas específicas.", "taskItem.assignedSummary", "Task item assigned summary", "No task item assignment summary found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Resumen de tareas específicas por responsable:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("responsiblePerson"))))
                    .append(" | total: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | completadas: ").append(blankToDash(value(row.get("completedItems"))))
                    .append(" | pendientes: ").append(blankToDash(value(row.get("pendingItems"))))
                    .append(" | tareas: ").append(blankToDash(value(row.get("taskNames"))));
        }
        return AiToolAnswer.of(answer.toString(), "taskItem.assignedSummary", "Task item assigned summary", "%d task item assignment summary rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer taskItemPrioritySummary() {
        List<Map<String, Object>> rows = taskItemRows(null, null, false, null, DEFAULT_LIMIT, "ti.sort_order ASC, tl.due_date ASC NULLS LAST, ti.task_name ASC", true);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré tareas específicas pendientes con prioridad alta. Para esta consulta estoy considerando prioridad alta como sort_order 0 o 1.",
                    "taskItem.prioritySummary",
                    "Task item priority summary",
                    "No high priority task items found using sort_order 0 or 1.",
                    List.of()
            );
        }
        return taskItemRowsAnswer(rows, "taskItem.prioritySummary", "Estas tareas específicas tienen prioridad alta según sort_order 0 o 1:");
    }



    private List<Map<String, Object>> documentRows(String search, String extraWhere, QueryConfigurer extraConfigurer, int limit, String orderBy) {
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
                  AND d.deleted_at IS NULL
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

    private AiToolAnswer documentRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, "Document metadata", "No document rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        appendDocumentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), toolName, "Document metadata", "%d document rows found.".formatted(rows.size()), rows);
    }

    private void appendDocumentRows(StringBuilder answer, List<Map<String, Object>> rows) {
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

    private String documentTypeFilterFromQuestion(String normalized) {
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

    private List<Map<String, Object>> reservationSupplyRows(String itemSearch, LocalDate fromDate, LocalDate toDate, String propertySearch, String reservationSearch, String status, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT rs.id,
                       rs.item_name_snapshot AS item_name,
                       rs.quantity,
                       COALESCE(rs.unit, '') AS unit,
                       COALESCE(rs.notes, '') AS notes,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.status
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                """);
        if (fromDate != null) {
            sql.append("  AND r.check_in >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND r.check_in <= :toDate\n");
        }
        if (status != null && !status.isBlank()) {
            sql.append("  AND r.status = :status\n");
        }
        if (itemSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:itemSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', rs.item_name_snapshot, rs.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', p.name, p.address, p.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (reservationSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:reservationSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', r.reservation_code, r.observations, p.name, r.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (fromDate != null) {
                q.setParameter("fromDate", Date.valueOf(fromDate));
            }
            if (toDate != null) {
                q.setParameter("toDate", Date.valueOf(toDate));
            }
            if (status != null && !status.isBlank()) {
                q.setParameter("status", status);
            }
            if (itemSearch != null) {
                q.setParameter("itemSearch", itemSearch);
            }
            if (propertySearch != null) {
                q.setParameter("propertySearch", propertySearch);
            }
            if (reservationSearch != null) {
                q.setParameter("reservationSearch", reservationSearch);
            }
            q.setParameter("limit", limit);
        }, "id", "itemName", "quantity", "unit", "notes", "propertyName", "reservationCode", "checkIn", "checkOut", "status");
    }

    private List<Map<String, Object>> reservationSupplySummaryRows(LocalDate fromDate, LocalDate toDate, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT rs.item_name_snapshot AS item_name,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(rs.quantity), 0) AS total_quantity,
                       COALESCE(MAX(rs.unit), '') AS unit,
                       MAX(r.check_in) AS last_check_in,
                       COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = rs.organization_id
                JOIN inventory_items ii ON ii.id = rs.inventory_item_id AND ii.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND ii.deleted_at IS NULL
                  AND ii.item_type = 'SUPPLY'
                """);
        if (fromDate != null) {
            sql.append("  AND r.check_in >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND r.check_in <= :toDate\n");
        }
        sql.append("""
                GROUP BY rs.item_name_snapshot
                ORDER BY usage_count DESC, total_quantity DESC, last_check_in DESC NULLS LAST, rs.item_name_snapshot ASC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (fromDate != null) {
                q.setParameter("fromDate", Date.valueOf(fromDate));
            }
            if (toDate != null) {
                q.setParameter("toDate", Date.valueOf(toDate));
            }
            q.setParameter("limit", limit);
        }, "itemName", "usageCount", "totalQuantity", "unit", "lastCheckIn", "properties");
    }

    private AiToolAnswer reservationSupplyRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | reserva: ").append(blankToDash(value(row.get("reservationCode"))))
                    .append(" | check-in: ").append(blankToDash(value(row.get("checkIn"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Reservation supply rows", "%d reservation supply rows found.".formatted(rows.size()), rows);
    }

    private String dateFilterSql(String column, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder();
        if (fromDate != null) {
            sql.append("  AND ").append(column).append(" >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND ").append(column).append(" <= :toDate\n");
        }
        return sql.toString();
    }

    private void setDateRangeParams(Query query, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            query.setParameter("fromDate", Date.valueOf(fromDate));
        }
        if (toDate != null) {
            query.setParameter("toDate", Date.valueOf(toDate));
        }
    }

    private List<Map<String, Object>> taskListRows(String search, String propertySearch, String reservationSearch, LocalDate fromDueDate, LocalDate toDueDate, List<String> statuses, LocalDate exactDueDate, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT tl.id,
                       p.name AS property_name,
                       COALESCE(r.reservation_code, '') AS reservation_code,
                       r.check_in,
                       tl.title,
                       tl.creation_date,
                       tl.due_date,
                       tl.status,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items,
                       COALESCE(SUM(CASE WHEN ti.completed = FALSE THEN 1 ELSE 0 END), 0) AS pending_items
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                LEFT JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = tl.organization_id AND r.deleted_at IS NULL
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', tl.title, tl.status, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', p.name, p.address, p.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (reservationSearch != null) {
            sql.append("""
                      AND tl.reservation_id IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:reservationSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', r.reservation_code, r.observations, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (fromDueDate != null) {
            sql.append("  AND tl.due_date >= :fromDueDate\n");
        }
        if (toDueDate != null) {
            sql.append("  AND tl.due_date <= :toDueDate\n");
        }
        if (exactDueDate != null) {
            sql.append("  AND tl.due_date = :exactDueDate\n");
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append("  AND tl.status IN (:statuses)\n");
        }
        sql.append(" GROUP BY tl.id, p.name, r.reservation_code, r.check_in, tl.title, tl.creation_date, tl.due_date, tl.status\n");
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            if (propertySearch != null) {
                q.setParameter("propertySearch", propertySearch);
            }
            if (reservationSearch != null) {
                q.setParameter("reservationSearch", reservationSearch);
            }
            if (fromDueDate != null) {
                q.setParameter("fromDueDate", Date.valueOf(fromDueDate));
            }
            if (toDueDate != null) {
                q.setParameter("toDueDate", Date.valueOf(toDueDate));
            }
            if (exactDueDate != null) {
                q.setParameter("exactDueDate", Date.valueOf(exactDueDate));
            }
            if (statuses != null && !statuses.isEmpty()) {
                q.setParameter("statuses", statuses);
            }
            q.setParameter("limit", limit);
        }, "id", "propertyName", "reservationCode", "checkIn", "title", "creationDate", "dueDate", "status", "totalItems", "completedItems", "pendingItems");
    }

    private AiToolAnswer taskListRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("dueDate"))))
                    .append(" | avance: ").append(blankToDash(value(row.get("completedItems"))))
                    .append("/").append(blankToDash(value(row.get("totalItems"))));
            if (!value(row.get("reservationCode")).isBlank()) {
                answer.append(" | reserva: ").append(blankToDash(value(row.get("reservationCode"))));
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Task list rows", "%d task list rows found.".formatted(rows.size()), rows);
    }

    private List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy) {
        return taskItemRows(search, taskListSearch, completed, overdueBefore, limit, orderBy, false);
    }

    private List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy, boolean highPriorityOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT ti.id,
                       ti.task_name,
                       ti.responsible_person,
                       ti.completed,
                       ti.completion_date,
                       ti.sort_order,
                       tl.title AS task_list_title,
                       tl.due_date,
                       tl.status AS task_list_status,
                       p.name AS property_name,
                       COALESCE(r.reservation_code, '') AS reservation_code
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = ti.organization_id
                LEFT JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = ti.organization_id AND r.deleted_at IS NULL
                WHERE ti.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', ti.task_name, ti.responsible_person, tl.title, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (taskListSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:taskListSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', tl.title, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (completed != null) {
            sql.append("  AND ti.completed = :completed\n");
        }
        if (overdueBefore != null) {
            sql.append("  AND tl.due_date <= :overdueBefore\n");
        }
        if (highPriorityOnly) {
            sql.append("  AND ti.sort_order IN (0, 1)\n");
        }
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            if (taskListSearch != null) {
                q.setParameter("taskListSearch", taskListSearch);
            }
            if (completed != null) {
                q.setParameter("completed", completed);
            }
            if (overdueBefore != null) {
                q.setParameter("overdueBefore", Date.valueOf(overdueBefore));
            }
            q.setParameter("limit", limit);
        }, "id", "taskName", "responsiblePerson", "completed", "completionDate", "sortOrder", "taskListTitle", "dueDate", "taskListStatus", "propertyName", "reservationCode");
    }

    private AiToolAnswer taskItemRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("taskName"))))
                    .append(" | lista: ").append(blankToDash(value(row.get("taskListTitle"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | completada: ").append(blankToDash(value(row.get("completed"))))
                    .append(" | vence lista: ").append(blankToDash(value(row.get("dueDate"))));
            if (!value(row.get("responsiblePerson")).isBlank()) {
                answer.append(" | responsable: ").append(blankToDash(value(row.get("responsiblePerson"))));
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Task item rows", "%d task item rows found.".formatted(rows.size()), rows);
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
        return documentRowsAnswer(rows, "document.failedProcessing", "Estos documentos fallaron al procesarse:", "No encontré documentos con procesamiento fallido.");
    }

    public AiToolAnswer processedDocuments() {
        List<Map<String, Object>> rows = documentRows(null, " AND d.processing_status = 'PROCESSED' ", null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.processed", "Estos documentos ya fueron procesados:", "No encontré documentos procesados.");
    }

    public AiToolAnswer indexedDocuments() {
        List<Map<String, Object>> rows = documentRows(null,
                " AND d.processing_status = 'PROCESSED' AND EXISTS (SELECT 1 FROM document_chunks dcx WHERE dcx.document_id = d.id AND dcx.organization_id = d.organization_id AND dcx.vector_store_id IS NOT NULL) ",
                null, DEFAULT_LIMIT, "d.created_at DESC");
        return documentRowsAnswer(rows, "document.indexed", "Estos documentos están listos para IA:", "No encontré documentos listos para IA.");
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
        return documentRowsAnswer(rows, "document.processedNotIndexed", "Estos documentos ya están procesados, pero todavía no están indexados para IA:", "No encontré documentos procesados pendientes de indexación IA.");
    }

    public AiToolAnswer documentCountByType() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT d.document_type,
                       COUNT(d.id) AS document_count,
                       COALESCE(SUM(CASE WHEN d.processing_status = 'PROCESSED' THEN 1 ELSE 0 END), 0) AS processed_count,
                       COALESCE(SUM(CASE WHEN EXISTS (
                           SELECT 1 FROM document_chunks dc
                           WHERE dc.document_id = d.id
                             AND dc.organization_id = d.organization_id
                             AND dc.vector_store_id IS NOT NULL
                       ) THEN 1 ELSE 0 END), 0) AS indexed_count,
                       COALESCE(STRING_AGG(d.title, ', ' ORDER BY d.title), '') AS document_titles
                FROM documents d
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                GROUP BY d.document_type
                ORDER BY document_count DESC, d.document_type ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "documentType", "documentCount", "processedCount", "indexedCount", "documentTitles");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré documentos para agrupar por tipo.", "document.countByType", "Document count by type", "No documents found.", List.of());
        }
        long totalDocuments = rows.stream().mapToLong(row -> toLong(row.get("documentCount"))).sum();
        StringBuilder answer = new StringBuilder("Tienes ").append(totalDocuments).append(" documentos cargados en total. Así se agrupan por tipo:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("documentType"))))
                    .append(" | documentos: ").append(blankToDash(value(row.get("documentCount"))))
                    .append(" | procesados: ").append(blankToDash(value(row.get("processedCount"))))
                    .append(" | listos para IA: ").append(blankToDash(value(row.get("indexedCount"))))
                    .append(System.lineSeparator())
                    .append("  Documentos: ").append(blankToDash(value(row.get("documentTitles"))));
        }
        return AiToolAnswer.of(answer.toString(), "document.countByType", "Document count by type", "%d document type rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer documentCountByProperty() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(p.name, 'Sin propiedad') AS property_name,
                       COUNT(d.id) AS document_count,
                       COALESCE(SUM(CASE WHEN d.processing_status = 'PROCESSED' THEN 1 ELSE 0 END), 0) AS processed_count,
                       COALESCE(SUM(CASE WHEN EXISTS (
                           SELECT 1 FROM document_chunks dc
                           WHERE dc.document_id = d.id
                             AND dc.organization_id = d.organization_id
                             AND dc.vector_store_id IS NOT NULL
                       ) THEN 1 ELSE 0 END), 0) AS indexed_count,
                       COALESCE(STRING_AGG(d.title, ', ' ORDER BY d.title), '') AS document_titles
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                GROUP BY p.name
                ORDER BY document_count DESC, property_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "propertyName", "documentCount", "processedCount", "indexedCount", "documentTitles");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré documentos para agrupar por propiedad.", "document.countByProperty", "Document count by property", "No documents found.", List.of());
        }
        long totalDocuments = rows.stream().mapToLong(row -> toLong(row.get("documentCount"))).sum();
        StringBuilder answer = new StringBuilder("Tienes ").append(totalDocuments).append(" documentos cargados en total. Así se agrupan por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | documentos: ").append(blankToDash(value(row.get("documentCount"))))
                    .append(" | procesados: ").append(blankToDash(value(row.get("processedCount"))))
                    .append(" | listos para IA: ").append(blankToDash(value(row.get("indexedCount"))))
                    .append(System.lineSeparator())
                    .append("  Documentos: ").append(blankToDash(value(row.get("documentTitles"))));
        }
        return AiToolAnswer.of(answer.toString(), "document.countByProperty", "Document count by property", "%d property document rows found.".formatted(rows.size()), rows);
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
                  AND d.deleted_at IS NULL
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
                  AND d.deleted_at IS NULL
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

    private List<Map<String, Object>> purchaseListRows(String search, List<String> statuses, PurchaseDateRange range, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT pl.id,
                       p.name AS property_name,
                       s.name AS supplier_name,
                       pl.purchase_date,
                       pl.status,
                       pl.notes,
                       COUNT(pi.id) AS item_count,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE THEN 1 ELSE 0 END), 0) AS purchased_item_count,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE THEN pi.estimated_price ELSE 0 END), 0) AS purchased_total_cost
                FROM purchase_lists pl
                LEFT JOIN properties p ON p.id = pl.property_id
                                      AND p.organization_id = pl.organization_id
                LEFT JOIN suppliers s ON s.id = pl.supplier_id
                                     AND s.organization_id = pl.organization_id
                                     AND s.deleted_at IS NULL
                LEFT JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                           AND pi.organization_id = pl.organization_id
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (statuses != null && !statuses.isEmpty()) {
            sql.append("  AND pl.status IN (:statuses)\n");
        }
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', p.name, s.name, pl.notes, pl.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY pl.id, p.name, s.name, pl.purchase_date, pl.status, pl.notes
                ORDER BY pl.purchase_date DESC, pl.created_at DESC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (statuses != null && !statuses.isEmpty()) {
                q.setParameter("statuses", statuses);
            }
            if (range != null) {
                q.setParameter("fromDate", Date.valueOf(range.fromDate()));
                q.setParameter("toDate", Date.valueOf(range.toDate()));
            }
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "id", "propertyName", "supplierName", "purchaseDate", "status", "notes", "itemCount", "purchasedItemCount", "purchasedTotalCost");
    }

    private List<Map<String, Object>> purchaseItemRows(String search, List<String> flags, PurchaseDateRange range, int limit) {
        boolean purchasedOnly = flags != null && flags.contains("purchasedOnly");
        StringBuilder sql = new StringBuilder("""
                SELECT pi.id,
                       pi.item_name_snapshot,
                       ii.item_type,
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
                                      AND pl.organization_id = pi.organization_id
                LEFT JOIN inventory_items ii ON ii.id = pi.inventory_item_id
                                            AND ii.organization_id = pi.organization_id
                LEFT JOIN properties p ON p.id = pl.property_id
                                      AND p.organization_id = pl.organization_id
                LEFT JOIN suppliers s ON s.id = pl.supplier_id
                                     AND s.organization_id = pl.organization_id
                                     AND s.deleted_at IS NULL
                WHERE pi.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (purchasedOnly) {
            sql.append("  AND pi.purchased = TRUE\n");
        }
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes, ii.name, ii.item_type, p.name, s.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                ORDER BY pl.purchase_date DESC, pi.created_at DESC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (range != null) {
                q.setParameter("fromDate", Date.valueOf(range.fromDate()));
                q.setParameter("toDate", Date.valueOf(range.toDate()));
            }
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "id", "itemName", "itemType", "quantity", "unit", "estimatedPrice", "purchased", "purchaseDate", "purchaseListStatus", "propertyName", "supplierName");
    }

    private String purchaseCostBaseSql(PurchaseDateRange range, String groupBy, String orderBy) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT pl.id) AS list_count,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(AVG(pi.estimated_price), 0) AS avg_line_cost,
                       MIN(pl.purchase_date) AS first_purchase_date,
                       MAX(pl.purchase_date) AS last_purchase_date
                FROM purchase_lists pl
                JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                      AND pi.organization_id = pl.organization_id
                                      AND pi.purchased = TRUE
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        return sql.toString();
    }

    private void setPurchaseCostCommonParams(Query query, PurchaseDateRange range) {
        query.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
        if (range != null) {
            query.setParameter("fromDate", Date.valueOf(range.fromDate()));
            query.setParameter("toDate", Date.valueOf(range.toDate()));
        }
    }

    private String purchaseItemAggregateSql(String search, String orderMetric, String direction) {
        String safeMetric = switch (orderMetric) {
            case "purchase_count" -> "purchase_count";
            case "total_quantity" -> "total_quantity";
            case "item_name" -> "item_name_snapshot";
            default -> "purchase_count";
        };
        String safeDirection = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        StringBuilder sql = new StringBuilder("""
                SELECT pi.item_name_snapshot,
                       COUNT(*) AS purchase_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(MAX(pi.unit), '') AS unit,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(AVG(pi.estimated_price), 0) AS average_line_cost,
                       COALESCE(AVG(CASE WHEN pi.quantity IS NOT NULL AND pi.quantity > 0 THEN pi.estimated_price / pi.quantity ELSE pi.estimated_price END), 0) AS average_unit_cost,
                       MIN(pl.purchase_date) AS first_purchase_date,
                       MAX(pl.purchase_date) AS last_purchase_date
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                                      AND pl.organization_id = pi.organization_id
                WHERE pi.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                  AND pi.purchased = TRUE
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY pi.item_name_snapshot
                """);
        sql.append("ORDER BY ").append(safeMetric).append(' ').append(safeDirection);
        if (!"purchase_count".equals(safeMetric)) {
            sql.append(", purchase_count ").append(safeDirection);
        }
        if (!"total_quantity".equals(safeMetric)) {
            sql.append(", total_quantity ").append(safeDirection);
        }
        sql.append(", pi.item_name_snapshot ASC\n");
        sql.append("LIMIT :limit\n");
        return sql.toString();
    }

    private PurchaseDateRange purchaseDateRange(String userQuestion) {
        String normalized = normalize(userQuestion);
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "este mes", "mes actual", "this month")) {
            LocalDate from = today.withDayOfMonth(1);
            LocalDate to = from.plusMonths(1).minusDays(1);
            return new PurchaseDateRange(from, to, "este mes");
        }
        if (containsAny(normalized, "mes pasado", "ultimo mes", "último mes", "last month")) {
            LocalDate from = today.minusMonths(1).withDayOfMonth(1);
            LocalDate to = from.plusMonths(1).minusDays(1);
            return new PurchaseDateRange(from, to, "el mes pasado");
        }
        if (containsAny(normalized, "esta semana", "semana actual", "this week")) {
            LocalDate from = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            return new PurchaseDateRange(from, from.plusDays(6), "esta semana");
        }
        if (containsAny(normalized, "hoy", "today")) {
            return new PurchaseDateRange(today, today, "hoy");
        }
        if (containsAny(normalized, "este ano", "este año", "year to date", "este anio")) {
            LocalDate from = today.withDayOfYear(1);
            return new PurchaseDateRange(from, today, "este año");
        }
        return null;
    }

    private void appendPurchaseListRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("purchaseDate"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))))
                    .append(" | items: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | comprados: ").append(blankToDash(value(row.get("purchasedItemCount"))))
                    .append(" | gasto comprado: ").append(formatMoney(row.get("purchasedTotalCost")));
        }
    }

    private void appendPurchaseItemRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("purchaseDate"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | precio: ").append(formatMoney(row.get("estimatedPrice")))
                    .append(" | comprado: ").append(blankToDash(value(row.get("purchased"))))
                    .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))));
        }
    }

    private record PurchaseDateRange(LocalDate fromDate, LocalDate toDate, String label) {
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


    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value(value);
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    @FunctionalInterface
    private interface QueryConfigurer {
        void configure(Query query);
    }

}
