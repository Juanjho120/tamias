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
public class ScheduledReservationGuestToolRepository extends AiReadOnlyToolSupport {

    public ScheduledReservationGuestToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
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
        String search = extractSearchText(userQuestion, "proximo", "proxima", "mantenimiento", "mantenimientos", "programado", "programados", "toca", "vence", "vencimiento");
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                SELECT sm.id,
                       p.name AS property_name,
                       sm.title,
                       COALESCE(mp.full_name, '') AS person_name,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
                       sm.start_date,
                       sm.end_date,
                       sm.next_due_date,
                       sm.frequency,
                       sm.interval_value,
                       sm.status
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id AND p.organization_id = sm.organization_id
                LEFT JOIN maintenance_people mp ON mp.id = sm.maintenance_person_id AND mp.organization_id = sm.organization_id
                LEFT JOIN maintenance_categories mc ON mc.id = sm.maintenance_category_id AND mc.organization_id = sm.organization_id
                LEFT JOIN maintenance_types mt ON mt.id = sm.maintenance_type_id AND mt.organization_id = sm.organization_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND sm.start_date > :today
                """ + (nullableSearch(search) == null ? "" : """
                  AND NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', sm.title, sm.description, p.name, mc.name, mt.name, sm.frequency, sm.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """) + """
                ORDER BY sm.start_date ASC, sm.next_due_date ASC, sm.title ASC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    if (nullableSearch(search) != null) {
                        q.setParameter("search", nullableSearch(search));
                    }
                }, "id", "propertyName", "title", "personName", "categoryName", "typeName", "startDate", "endDate", "nextDueDate", "frequency", "intervalValue", "status");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré información sobre próximos mantenimientos programados. Si necesitas más detalles o tienes otra consulta, no dudes en preguntar.",
                    "scheduledMaintenance.nextDue",
                    "Next scheduled maintenance",
                    "No upcoming scheduled maintenance rows found by start date.",
                    List.of()
            );
        }
        Map<String, Object> row = rows.get(0);
        String answer = "El próximo mantenimiento programado por fecha de inicio es:\n"
                + "- " + blankToDash(value(row.get("propertyName")))
                + " | " + blankToDash(value(row.get("title")))
                + " | inicio: " + blankToDash(value(row.get("startDate")))
                + " | finalización: " + blankToDash(value(row.get("endDate")))
                + " | próximo vencimiento: " + blankToDash(value(row.get("nextDueDate")))
                + " | responsable: " + blankToDash(value(row.get("personName")))
                + " | categoría/tipo: " + blankToDash(value(row.get("categoryName")))
                + (value(row.get("typeName")).isBlank() ? "" : " / " + value(row.get("typeName")))
                + " | estado: " + blankToDash(value(row.get("status")));
        return AiToolAnswer.of(answer, "scheduledMaintenance.nextDue", "Next scheduled maintenance", "Next scheduled maintenance was selected by start date.", rows);
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

    public AiToolAnswer guestSearch(String userQuestion) {
        String search = extractSearchText(userQuestion,
                "huesped", "huespedes", "cliente", "clientes", "buscar", "busca", "lista", "listar",
                "que", "sabes", "sobre", "quien", "es", "dame", "informacion", "información", "del", "de", "la", "el"
        );
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
        Map<String, List<Map<String, Object>>> byProperty = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String property = blankToDash(value(row.get("propertyName")));
            byProperty.computeIfAbsent(property, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> propertyEntry : byProperty.entrySet()) {
            answer.append(System.lineSeparator())
                    .append("- ").append(propertyEntry.getKey());
            for (Map<String, Object> row : propertyEntry.getValue()) {
                answer.append(System.lineSeparator())
                        .append("  - ").append(blankToDash(value(row.get("title"))))
                        .append(" | vencía el ").append(blankToDash(value(row.get("nextDueDate"))))
                        .append(" | días vencido: ").append(blankToDash(value(row.get("daysOverdue"))));
            }
        }
        return AiToolAnswer.of(
                answer.toString(),
                "scheduledMaintenance.overdue",
                "Overdue scheduled maintenance",
                "%d overdue scheduled maintenance records found.".formatted(rows.size()),
                rows
        );
    }
}
