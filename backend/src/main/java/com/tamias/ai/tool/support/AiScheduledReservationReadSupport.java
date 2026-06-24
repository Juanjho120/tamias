package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AiScheduledReservationReadSupport extends AiToolFormattingSupport {

    protected AiScheduledReservationReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected AiToolAnswer scheduledMaintenanceList(String toolName, String label, String emptyOrIntro, LocalDate from, LocalDate to, String search) {
        return scheduledMaintenanceList(toolName, label, emptyOrIntro, from, to, search, DEFAULT_LIMIT);
    }

    protected AiToolAnswer scheduledMaintenanceList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
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



































    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search) {
        return reservationList(toolName, label, intro, from, to, search, DEFAULT_LIMIT, false);
    }

    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit, boolean currentOnly) {
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

    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
        return reservationList(toolName, label, intro, from, to, search, limit, false);
    }

    protected String reservationBaseSql(String whereClause, String orderBy, int limit) {
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

    protected void appendOptionalReservationDateFilters(StringBuilder sql, LocalDate[] range) {
        if (range[0] != null) {
            sql.append(" AND r.check_in >= :fromDate\n");
        }
        if (range[1] != null) {
            sql.append(" AND r.check_in <= :toDate\n");
        }
    }

    protected void setOptionalReservationDateParameters(Query query, LocalDate[] range) {
        if (range[0] != null) {
            query.setParameter("fromDate", Date.valueOf(range[0]));
        }
        if (range[1] != null) {
            query.setParameter("toDate", Date.valueOf(range[1]));
        }
    }

    protected String[] reservationColumns() {
        return new String[]{"id", "propertyName", "platformName", "reservationCode", "checkIn", "checkOut", "reservationValue", "status", "guests", "guestCount"};
    }

    protected AiToolAnswer reservationRowsAnswer(List<Map<String, Object>> rows, String toolName, String label, String intro, String emptyMessage) {
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













    protected AiToolAnswer guestList(String toolName, String label, String intro, String search, boolean upcomingOnly, boolean recentOnly) {
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

    protected AiToolAnswer guestReservationList(String toolName, String label, String intro, String search, boolean upcomingOnly) {
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

    protected String resolveScheduledMaintenanceStatus(String userQuestion) {
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

    protected String resolveReservationStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "cancelado", "cancelados", "cancelada", "canceladas", "cancelled", "canceled")) {
            return "CANCELLED";
        }
        if (containsAny(normalized, "eliminado", "eliminados", "deleted")) {
            return "DELETED";
        }
        return "ACTIVE";
    }

    protected LocalDate[] resolveDateRange(String userQuestion) {
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
}
