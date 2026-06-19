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
public class ReservationSupplyTaskToolRepository extends AiReadOnlyToolSupport {

    public ReservationSupplyTaskToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
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
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                WITH next_reservation AS (
                    SELECT r.id
                    FROM reservations r
                    WHERE r.organization_id = :organizationId
                      AND r.deleted_at IS NULL
                      AND r.status = 'ACTIVE'
                      AND r.check_in >= :today
                      AND EXISTS (
                          SELECT 1
                          FROM reservation_supplies rsx
                          WHERE rsx.organization_id = r.organization_id
                            AND rsx.reservation_id = r.id
                      )
                    ORDER BY r.check_in ASC, r.created_at ASC
                    LIMIT 1
                )
                SELECT rs.id,
                       rs.item_name_snapshot AS item_name,
                       rs.quantity,
                       COALESCE(rs.unit, '') AS unit,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       COALESCE(
                           MAX(CASE WHEN rg.is_primary = TRUE THEN g.full_name ELSE NULL END),
                           MIN(g.full_name),
                           ''
                       ) AS primary_guest
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN next_reservation nr ON nr.id = r.id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                GROUP BY rs.id, rs.item_name_snapshot, rs.quantity, rs.unit, p.name, r.reservation_code, r.check_in, r.check_out
                ORDER BY rs.item_name_snapshot ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "itemName", "quantity", "unit", "propertyName", "reservationCode", "checkIn", "checkOut", "primaryGuest");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré supplies asignados para la próxima reservación activa.",
                    "reservationSupply.forUpcomingReservations",
                    "Reservation supplies for next reservation",
                    "No supplies found for next reservation.",
                    List.of()
            );
        }
        Map<String, Object> first = rows.get(0);
        StringBuilder answer = new StringBuilder("Para la próxima reserva de ")
                .append(blankToDash(value(first.get("primaryGuest"))))
                .append(" en ").append(blankToDash(value(first.get("propertyName"))))
                .append(" con check-in ").append(blankToDash(value(first.get("checkIn"))))
                .append(", necesitas los siguientes supplies:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity"))))
                    .append(" ").append(blankToDash(value(row.get("unit"))));
        }
        return AiToolAnswer.of(answer.toString(), "reservationSupply.forUpcomingReservations", "Reservation supplies for next reservation", "%d supplies found for next reservation.".formatted(rows.size()), rows);
    }

    public AiToolAnswer reservationSuppliesForLatestPastReservation() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> reservations = query("""
                SELECT r.id,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       COALESCE(
                           MAX(CASE WHEN rg.is_primary = TRUE THEN g.full_name ELSE NULL END),
                           MIN(g.full_name),
                           ''
                       ) AS primary_guest
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in <= :today
                GROUP BY r.id, p.name, r.reservation_code, r.check_in, r.check_out
                ORDER BY r.check_in DESC, r.created_at DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }, "id", "propertyName", "reservationCode", "checkIn", "checkOut", "primaryGuest");
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
        String primaryGuest = blankToDash(value(reservation.get("primaryGuest")));
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré supplies asociados a la última reservación " + reservationLabel + " de " + propertyName + " con check-in " + checkIn + ".",
                    "reservationSupply.byReservation",
                    "Reservation supplies for latest past reservation",
                    "Latest past reservation found, but no supplies were assigned.",
                    reservations
            );
        }
        StringBuilder answer = new StringBuilder("En la última reservación se usaron los siguientes supplies:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity"))))
                    .append(" ").append(blankToDash(value(row.get("unit"))));
        }
        answer.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append("La reservación corresponde a ").append(reservationLabel)
                .append(" | ").append(propertyName)
                .append(", a nombre de ").append(primaryGuest)
                .append(" con check-in el ").append(checkIn)
                .append(".");
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
                       tl.due_date,
                       tl.status,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = tl.organization_id AND r.deleted_at IS NULL
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.reservation_id = :reservationId
                GROUP BY tl.id, p.name, r.reservation_code, r.check_in, tl.title, tl.due_date, tl.status
                ORDER BY tl.due_date ASC NULLS LAST, tl.creation_date DESC, tl.title ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("reservationId", reservationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "reservationCode", "checkIn", "title", "dueDate", "status", "totalItems", "completedItems");
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
        StringBuilder answer = new StringBuilder("Para la próxima reservación ")
                .append(reservationLabel)
                .append(" de ").append(propertyName)
                .append(" con check-in el ").append(checkIn)
                .append(", hay ").append(rows.size() == 1 ? "una lista de tarea pendiente:" : rows.size() + " listas de tareas pendientes:");
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            answer.append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(i + 1).append(". ").append(blankToDash(value(row.get("title"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("dueDate"))))
                    .append(" | avance: ").append(blankToDash(value(row.get("completedItems"))))
                    .append("/").append(blankToDash(value(row.get("totalItems"))))
                    .append(".");
            UUID taskListId = UUID.fromString(value(row.get("id")));
            appendTaskItemsForTaskList(answer, organizationId, taskListId);
        }
        return AiToolAnswer.of(answer.toString(), "taskList.byReservation", "Task lists for next reservation", "%d task lists found for next reservation.".formatted(rows.size()), rows);
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
        return groupedPendingTaskItemsAnswer(rows, "taskItem.pending", "Tienes las siguientes tareas pendientes:");
    }

    public AiToolAnswer completedTaskItems() {
        List<Map<String, Object>> rows = taskItemRows(null, null, true, null, DEFAULT_LIMIT, "p.name ASC, tl.due_date ASC NULLS LAST, tl.title ASC, COALESCE(ti.responsible_person, '') ASC, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas completadas.", "taskItem.completed", "Completed task items", "No completed task items found.", List.of());
        }
        return groupedCompletedTaskItemsAnswer(rows);
    }

    public AiToolAnswer overdueTaskItems() {
        List<Map<String, Object>> rows = taskItemRows(null, null, false, LocalDate.now().minusDays(1), DEFAULT_LIMIT, "tl.due_date ASC NULLS LAST, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas atrasadas.", "taskItem.overdue", "Overdue task items", "No overdue task items found.", List.of());
        }
        return taskItemRowsAnswer(rows, "taskItem.overdue", "Estas tareas específicas están atrasadas:");
    }

    public AiToolAnswer taskItemAssignedSummary() {
        List<Map<String, Object>> rows = taskItemRows(null, null, false, null, 50, "COALESCE(ti.responsible_person, '') ASC, p.name ASC, COALESCE(r.reservation_code, '') ASC, tl.due_date ASC NULLS LAST, tl.title ASC, ti.sort_order ASC, ti.task_name ASC");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré tareas específicas pendientes asignadas por responsable.", "taskItem.assignedSummary", "Task item assigned summary", "No pending task item assignment summary found.", List.of());
        }

        StringBuilder answer = new StringBuilder("Estas son las tareas específicas pendientes asignadas por persona:");
        Map<String, List<Map<String, Object>>> byResponsible = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String responsible = blankToDash(value(row.get("responsiblePerson")));
            byResponsible.computeIfAbsent(responsible, ignored -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<Map<String, Object>>> responsibleEntry : byResponsible.entrySet()) {
            answer.append(System.lineSeparator()).append(System.lineSeparator()).append(responsibleEntry.getKey()).append(":");

            Map<String, List<Map<String, Object>>> byProperty = new LinkedHashMap<>();
            for (Map<String, Object> row : responsibleEntry.getValue()) {
                String property = blankToDash(value(row.get("propertyName")));
                byProperty.computeIfAbsent(property, ignored -> new ArrayList<>()).add(row);
            }

            for (Map.Entry<String, List<Map<String, Object>>> propertyEntry : byProperty.entrySet()) {
                answer.append(System.lineSeparator()).append("- ").append(propertyEntry.getKey());

                Map<String, List<Map<String, Object>>> byReservation = new LinkedHashMap<>();
                for (Map<String, Object> row : propertyEntry.getValue()) {
                    String reservationCode = value(row.get("reservationCode"));
                    String reservationKey = reservationCode.isBlank() ? "__NO_RESERVATION__" : reservationCode;
                    byReservation.computeIfAbsent(reservationKey, ignored -> new ArrayList<>()).add(row);
                }

                for (Map.Entry<String, List<Map<String, Object>>> reservationEntry : byReservation.entrySet()) {
                    boolean hasReservation = !"__NO_RESERVATION__".equals(reservationEntry.getKey());
                    if (hasReservation) {
                        String primaryGuest = blankToDash(value(reservationEntry.getValue().getFirst().get("primaryGuest")));
                        answer.append(System.lineSeparator())
                                .append("   ")
                                .append(reservationEntry.getKey())
                                .append(" (")
                                .append(primaryGuest)
                                .append(")");
                    }

                    Map<String, List<Map<String, Object>>> byTaskList = new LinkedHashMap<>();
                    for (Map<String, Object> row : reservationEntry.getValue()) {
                        String listKey = blankToDash(value(row.get("taskListTitle"))) + " | vence: " + blankToDash(value(row.get("dueDate")));
                        byTaskList.computeIfAbsent(listKey, ignored -> new ArrayList<>()).add(row);
                    }

                    for (Map.Entry<String, List<Map<String, Object>>> taskListEntry : byTaskList.entrySet()) {
                        answer.append(System.lineSeparator())
                                .append(hasReservation ? "      - " : "   - ")
                                .append(taskListEntry.getKey());
                        for (Map<String, Object> row : taskListEntry.getValue()) {
                            answer.append(System.lineSeparator())
                                    .append(hasReservation ? "         - " : "      - ")
                                    .append(blankToDash(value(row.get("taskName"))));
                        }
                    }
                }
            }
        }
        return AiToolAnswer.of(answer.toString(), "taskItem.assignedSummary", "Task item assigned summary", "%d pending task item assignment rows found.".formatted(rows.size()), rows);
    }

    private AiToolAnswer groupedCompletedTaskItemsAnswer(List<Map<String, Object>> rows) {
        StringBuilder answer = new StringBuilder("Estas tareas específicas ya están completadas:");
        Map<String, List<Map<String, Object>>> byProperty = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String property = blankToDash(value(row.get("propertyName")));
            byProperty.computeIfAbsent(property, ignored -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<Map<String, Object>>> propertyEntry : byProperty.entrySet()) {
            answer.append(System.lineSeparator()).append("- ").append(propertyEntry.getKey());

            Map<String, List<Map<String, Object>>> byTaskList = new LinkedHashMap<>();
            for (Map<String, Object> row : propertyEntry.getValue()) {
                String listKey = blankToDash(value(row.get("taskListTitle"))) + " | vence lista: " + blankToDash(value(row.get("dueDate")));
                byTaskList.computeIfAbsent(listKey, ignored -> new ArrayList<>()).add(row);
            }

            for (Map.Entry<String, List<Map<String, Object>>> taskListEntry : byTaskList.entrySet()) {
                answer.append(System.lineSeparator()).append("  - ").append(taskListEntry.getKey());

                Map<String, List<Map<String, Object>>> byResponsible = new LinkedHashMap<>();
                for (Map<String, Object> row : taskListEntry.getValue()) {
                    String responsible = blankToDash(value(row.get("responsiblePerson")));
                    byResponsible.computeIfAbsent(responsible, ignored -> new ArrayList<>()).add(row);
                }

                for (Map.Entry<String, List<Map<String, Object>>> responsibleEntry : byResponsible.entrySet()) {
                    answer.append(System.lineSeparator()).append("    - ").append(responsibleEntry.getKey());
                    for (Map<String, Object> row : responsibleEntry.getValue()) {
                        answer.append(System.lineSeparator())
                                .append("      - ")
                                .append(blankToDash(value(row.get("taskName"))));
                    }
                }
            }
        }

        return AiToolAnswer.of(answer.toString(), "taskItem.completed", "Completed task items", "%d completed task item rows found.".formatted(rows.size()), rows);
    }

    private AiToolAnswer groupedPendingTaskItemsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        Map<String, List<Map<String, Object>>> byProperty = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String property = blankToDash(value(row.get("propertyName")));
            byProperty.computeIfAbsent(property, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> propertyEntry : byProperty.entrySet()) {
            answer.append(System.lineSeparator()).append(System.lineSeparator()).append(propertyEntry.getKey());
            Map<String, List<Map<String, Object>>> byList = new LinkedHashMap<>();
            for (Map<String, Object> row : propertyEntry.getValue()) {
                String listKey = blankToDash(value(row.get("taskListTitle"))) + " | vence el " + blankToDash(value(row.get("dueDate")));
                byList.computeIfAbsent(listKey, ignored -> new ArrayList<>()).add(row);
            }
            int listIndex = 1;
            for (Map.Entry<String, List<Map<String, Object>>> listEntry : byList.entrySet()) {
                answer.append(System.lineSeparator()).append(listIndex++).append(". ").append(listEntry.getKey());
                Map<String, List<Map<String, Object>>> byResponsible = new LinkedHashMap<>();
                for (Map<String, Object> row : listEntry.getValue()) {
                    String responsible = blankToDash(value(row.get("responsiblePerson")));
                    byResponsible.computeIfAbsent(responsible, ignored -> new ArrayList<>()).add(row);
                }
                for (Map.Entry<String, List<Map<String, Object>>> responsibleEntry : byResponsible.entrySet()) {
                    answer.append(System.lineSeparator()).append("   ").append(responsibleEntry.getKey());
                    for (Map<String, Object> row : responsibleEntry.getValue()) {
                        answer.append(System.lineSeparator()).append("      - ").append(blankToDash(value(row.get("taskName"))));
                    }
                }
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Pending task items", "%d pending task item rows found.".formatted(rows.size()), rows);
    }

    private void appendTaskItemsForTaskList(StringBuilder answer, UUID organizationId, UUID taskListId) {
        List<Map<String, Object>> items = query("""
                SELECT ti.task_name,
                       COALESCE(ti.responsible_person, 'Sin responsable') AS responsible_person,
                       ti.completed
                FROM task_items ti
                WHERE ti.organization_id = :organizationId
                  AND ti.task_list_id = :taskListId
                  AND ti.completed = FALSE
                ORDER BY COALESCE(ti.responsible_person, 'Sin responsable') ASC, ti.sort_order ASC, ti.task_name ASC
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("taskListId", taskListId);
                }, "taskName", "responsiblePerson", "completed");
        Map<String, List<Map<String, Object>>> byResponsible = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String responsible = blankToDash(value(item.get("responsiblePerson")));
            byResponsible.computeIfAbsent(responsible, ignored -> new ArrayList<>()).add(item);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : byResponsible.entrySet()) {
            answer.append(System.lineSeparator()).append(entry.getKey());
            for (Map<String, Object> item : entry.getValue()) {
                answer.append(System.lineSeparator()).append("- ").append(blankToDash(value(item.get("taskName"))));
            }
        }
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
}
