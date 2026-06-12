package com.tamias.ai.tool;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
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
            "a", "al", "algo", "actual", "actuales", "actualmente", "cargado", "cargados", "con", "cual",
            "cuales", "cuanto", "cuantos", "da", "dame", "de", "del", "dice", "el", "en", "estado",
            "estan", "esta", "este", "estos", "fue", "hay", "indexado", "indexados", "la", "las", "le",
            "lista", "listar", "lo", "los", "me", "mi", "mis", "muestra", "nombre", "para", "por", "procesado",
            "procesados", "que", "registrada", "registradas", "registrado", "registrados", "son", "subido",
            "subidos", "tengo", "tienes", "tipo", "tu", "un", "una", "ver"
    );

    private final EntityManager entityManager;
    private final CurrentUserService currentUserService;

    public AiReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
    }

    public AiToolAnswer capabilities() {
        String answer = """
                Soy el asistente IA de TAMIAS.

                Puedo ayudarte a consultar información operativa de tus propiedades, reservas, mantenimientos, compras, tareas, documentos e inventario. Puedo responder preguntas como:
                - Qué reservas tienes esta semana.
                - Cuándo fue el último mantenimiento de una propiedad.
                - Qué compras hiciste recientemente.
                - Qué tareas están pendientes.
                - Qué documentos están procesados o indexados para IA.
                - Qué dicen tus documentos indexados, como reglas de la casa o manuales.

                Por seguridad, en esta versión solo puedo consultar y resumir información. No puedo crear, editar, eliminar registros ni enviar notificaciones automáticamente.
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
                SELECT u.first_name,
                       u.last_name,
                       u.email,
                       u.status,
                       u.password_change_required,
                       r.code AS role_code,
                       o.name AS organization_name
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
            answer = "No encontré un número de teléfono registrado en tu perfil de TAMIAS.";
        } else if (containsAny(normalizedQuestion, "correo", "email", "usuario")) {
            answer = "El correo actual que estás usando es " + blankToDash(email) + ".";
        } else if (containsAny(normalizedQuestion, "rol")) {
            answer = "Tu rol actual en TAMIAS es " + blankToDash(role) + ".";
        } else if (containsAny(normalizedQuestion, "organizacion")) {
            answer = "Tu organización actual en TAMIAS es " + blankToDash(organizationName) + ".";
        } else if (containsAny(normalizedQuestion, "nombre", "llamo")) {
            answer = "Te llamas " + blankToDash(fullName) + ".";
        } else {
            answer = """
                    Tu perfil actual en TAMIAS es:

                    - Nombre: %s
                    - Correo/usuario: %s
                    - Rol: %s
                    - Organización: %s

                    No voy a mostrar datos sensibles como contraseñas, tokens o información interna de seguridad.
                    """.formatted(
                    blankToDash(fullName),
                    blankToDash(email),
                    blankToDash(role),
                    blankToDash(organizationName)
            ).trim();
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
                SELECT o.name,
                       o.description,
                       o.status,
                       COUNT(DISTINCT uo.user_id) AS user_count
                FROM organizations o
                LEFT JOIN user_organizations uo ON uo.organization_id = o.id
                                                AND uo.status = 'ACTIVE'
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
        String answer = """
                Estás trabajando en la organización:

                - Nombre: %s
                - Estado: %s
                - Usuarios activos asociados: %s
                """.formatted(
                blankToDash(value(row.get("name"))),
                blankToDash(value(row.get("status"))),
                blankToDash(value(row.get("userCount")))
        ).trim();

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
        String search = nullableSearch(extractSearchText(userQuestion));

        List<Map<String, Object>> rows = query("""
                SELECT p.id,
                       p.name,
                       p.status,
                       p.address,
                       p.description
                FROM properties p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
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
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "status", "address", "description");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré propiedades que coincidan con tu pregunta.",
                    "property.search",
                    "Properties",
                    "No matching properties found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Encontré estas propiedades en tu organización:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("name"))))
                    .append(" — ")
                    .append(blankToDash(value(row.get("status"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "property.search",
                "Properties",
                "%d properties found.".formatted(rows.size()),
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
                """, organizationId));
        summary.put("upcomingReservations7Days", scalar("""
                SELECT COUNT(*)
                FROM reservations
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND check_in BETWEEN :today AND :nextSevenDays
                """, organizationId, today, nextSevenDays));
        summary.put("overdueScheduledMaintenance", scalar("""
                SELECT COUNT(*)
                FROM scheduled_maintenance
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND next_due_date < :today
                """, organizationId, today));
        summary.put("openTaskLists", scalar("""
                SELECT COUNT(*)
                FROM task_lists
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status IN ('OPEN', 'IN_PROGRESS')
                """, organizationId));
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
                """, organizationId));

        List<Map<String, Object>> rows = List.of(summary);
        String answer = """
                Resumen operativo actual:

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
                       COALESCE(STRING_AGG(CONCAT(g.first_name, ' ', g.last_name), ', ' ORDER BY g.first_name, g.last_name), '') AS guests
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN guests g ON g.reservation_id = r.id
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

        StringBuilder answer = new StringBuilder("Estas son las próximas reservaciones activas:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ")
                    .append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ")
                    .append(blankToDash(value(row.get("checkOut"))));

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
        String search = nullableSearch(extractSearchText(userQuestion));

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
                  AND (
                    CAST(:search AS TEXT) IS NULL
                    OR LOWER(mr.title) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                    OR LOWER(COALESCE(mr.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                    OR LOWER(COALESCE(mc.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                    OR LOWER(COALESCE(mt.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "propertyName", "title", "description", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré mantenimientos que coincidan con tu pregunta.",
                    "maintenance.lastPerformed",
                    "Last performed maintenance",
                    "No matching maintenance record found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = """
                El mantenimiento más reciente que encontré es:

                - Propiedad: %s
                - Título: %s
                - Categoría: %s
                - Tipo: %s
                - Fecha realizada: %s
                - Fecha programada: %s
                - Estado: %s
                - Costo: %s
                """.formatted(
                blankToDash(value(row.get("propertyName"))),
                blankToDash(value(row.get("title"))),
                blankToDash(value(row.get("categoryName"))),
                blankToDash(value(row.get("typeName"))),
                blankToDash(value(row.get("performedAt"))),
                blankToDash(value(row.get("scheduledAt"))),
                blankToDash(value(row.get("status"))),
                formatMoney(row.get("cost"))
        ).trim();

        return AiToolAnswer.of(
                answer,
                "maintenance.lastPerformed",
                "Last performed maintenance",
                "Most recent matching maintenance record found.",
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

        StringBuilder answer = new StringBuilder("Encontré estos mantenimientos programados vencidos:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ")
                    .append(blankToDash(value(row.get("title"))))
                    .append(" | vencía el ")
                    .append(blankToDash(value(row.get("nextDueDate"))))
                    .append(" | días vencido: ")
                    .append(blankToDash(value(row.get("daysOverdue"))));
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
        String search = nullableSearch(extractPurchaseSearchText(userQuestion));

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
                  AND pl.deleted_at IS NULL
                  AND pi.purchased = TRUE
                  AND (
                    CAST(:search AS TEXT) IS NULL
                    OR LOWER(pi.item_name_snapshot) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                    OR LOWER(COALESCE(pi.notes, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                ORDER BY pl.purchase_date DESC, pi.created_at DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "itemName", "quantity", "unit", "estimatedPrice", "purchased", "purchaseDate", "purchaseListStatus", "propertyName", "supplierName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré una compra registrada que coincida con tu pregunta.",
                    "purchaseItem.lastPurchased",
                    "Last purchased item",
                    "No matching purchased item found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = """
                La última compra que encontré es:

                - Item: %s
                - Fecha de compra: %s
                - Cantidad: %s %s
                - Precio estimado: %s
                - Propiedad: %s
                - Proveedor: %s
                """.formatted(
                blankToDash(value(row.get("itemName"))),
                blankToDash(value(row.get("purchaseDate"))),
                blankToDash(value(row.get("quantity"))),
                blankToDash(value(row.get("unit"))),
                formatMoney(row.get("estimatedPrice")),
                blankToDash(value(row.get("propertyName"))),
                blankToDash(value(row.get("supplierName")))
        ).trim();

        return AiToolAnswer.of(
                answer,
                "purchaseItem.lastPurchased",
                "Last purchased item",
                "Most recent matching purchased item found.",
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
                       SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END) AS completed_items
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

        StringBuilder answer = new StringBuilder("Estas son las listas de tareas pendientes o en progreso:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("title"))))
                    .append(" | ")
                    .append(blankToDash(value(row.get("propertyName"))))
                    .append(" | estado: ")
                    .append(blankToDash(value(row.get("status"))))
                    .append(" | avance: ")
                    .append(blankToDash(value(row.get("completedItems"))))
                    .append("/")
                    .append(blankToDash(value(row.get("totalItems"))));
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
        String search = nullableSearch(extractSearchText(userQuestion));

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
                       SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END) AS indexed_chunk_count
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
                    "No encontré documentos que coincidan con tu pregunta.",
                    "document.searchMetadata",
                    "Document metadata",
                    "No matching documents found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Encontré estos documentos:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("title"))))
                    .append(" | tipo: ")
                    .append(blankToDash(value(row.get("documentType"))))
                    .append(" | procesamiento: ")
                    .append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | chunks indexados: ")
                    .append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/")
                    .append(blankToDash(value(row.get("chunkCount"))));
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
                       SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END) AS indexed_chunk_count,
                       SUM(CASE WHEN dc.vector_store_id IS NULL THEN 1 ELSE 0 END) AS missing_vector_id_count
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

        StringBuilder answer = new StringBuilder("Estado de indexación IA de documentos:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("title"))))
                    .append(" | procesamiento: ")
                    .append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | chunks indexados: ")
                    .append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/")
                    .append(blankToDash(value(row.get("chunkCount"))))
                    .append(" | chunks sin vector_store_id: ")
                    .append(blankToDash(value(row.get("missingVectorIdCount"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "rag.documentIndexStatus",
                "RAG document index status",
                "%d document index statuses found.".formatted(rows.size()),
                rows
        );
    }

    private Object scalar(String sql, UUID organizationId) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("organizationId", organizationId);
        return normalizeValue(query.getSingleResult());
    }

    private Object scalar(String sql, UUID organizationId, LocalDate today) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("organizationId", organizationId);
        query.setParameter("today", Date.valueOf(today));
        return normalizeValue(query.getSingleResult());
    }

    private Object scalar(String sql, UUID organizationId, LocalDate today, LocalDate until) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("organizationId", organizationId);
        query.setParameter("today", Date.valueOf(today));
        query.setParameter("nextSevenDays", Date.valueOf(until));
        return normalizeValue(query.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> query(String sql, QueryConfigurer configurer, String... columns) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        List<Object> resultList = query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Object result : resultList) {
            Object[] values = result instanceof Object[] array ? array : new Object[] { result };
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
        if (value instanceof java.sql.Timestamp timestamp) {
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

    private String extractSearchText(String userQuestion) {
        if (userQuestion == null) {
            return "";
        }

        String cleaned = normalize(userQuestion)
                .replace("ultimo", " ")
                .replace("ultima", " ")
                .replace("mantenimiento", " ")
                .replace("mantenimientos", " ")
                .replace("propiedad", " ")
                .replace("propiedades", " ")
                .replace("documento", " ")
                .replace("documentos", " ")
                .replaceAll("[^a-z0-9\\s-]", " ");

        return trimSearch(cleanStopWords(cleaned));
    }

    private String extractPurchaseSearchText(String userQuestion) {
        if (userQuestion == null) {
            return "";
        }

        String cleaned = normalize(userQuestion)
                .replace("cuando", " ")
                .replace("compre", " ")
                .replace("compraste", " ")
                .replace("compro", " ")
                .replace("compra", " ")
                .replace("compras", " ")
                .replace("comprado", " ")
                .replace("ultimo", " ")
                .replace("ultima", " ")
                .replace("vez", " ")
                .replace("por", " ")
                .replaceAll("[^a-z0-9\\s-]", " ");

        return trimSearch(cleanStopWords(cleaned));
    }

    private String cleanStopWords(String value) {
        return Arrays.stream(value.split("\\s+"))
                .map(String::trim)
                .filter(word -> !word.isBlank())
                .filter(word -> !SEARCH_STOP_WORDS.contains(word))
                .collect(Collectors.joining(" "));
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
