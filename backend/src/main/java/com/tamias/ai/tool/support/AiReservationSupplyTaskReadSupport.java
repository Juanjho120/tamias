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

public abstract class AiReservationSupplyTaskReadSupport extends AiDocumentReadSupport {

    protected AiReservationSupplyTaskReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> reservationSupplyRows(String itemSearch, LocalDate fromDate, LocalDate toDate, String propertySearch, String reservationSearch, String status, int limit, String orderBy) {
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

    protected List<Map<String, Object>> reservationSupplySummaryRows(LocalDate fromDate, LocalDate toDate, int limit) {
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

    protected AiToolAnswer reservationSupplyRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
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

    protected String dateFilterSql(String column, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder();
        if (fromDate != null) {
            sql.append("  AND ").append(column).append(" >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND ").append(column).append(" <= :toDate\n");
        }
        return sql.toString();
    }

    protected void setDateRangeParams(Query query, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            query.setParameter("fromDate", Date.valueOf(fromDate));
        }
        if (toDate != null) {
            query.setParameter("toDate", Date.valueOf(toDate));
        }
    }

    protected List<Map<String, Object>> taskListRows(String search, String propertySearch, String reservationSearch, LocalDate fromDueDate, LocalDate toDueDate, List<String> statuses, LocalDate exactDueDate, int limit, String orderBy) {
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

    protected AiToolAnswer taskListRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
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

    protected List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy) {
        return taskItemRows(search, taskListSearch, completed, overdueBefore, limit, orderBy, false);
    }

    protected List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy, boolean highPriorityOnly) {
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
                       COALESCE(r.reservation_code, '') AS reservation_code,
                       COALESCE((
                           SELECT g.full_name
                           FROM reservation_guests rg
                           JOIN guests g ON g.id = rg.guest_id
                                        AND g.organization_id = rg.organization_id
                                        AND g.deleted_at IS NULL
                           WHERE rg.reservation_id = r.id
                             AND rg.organization_id = ti.organization_id
                           ORDER BY rg.is_primary DESC, g.full_name ASC
                           LIMIT 1
                       ), '') AS primary_guest
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
        }, "id", "taskName", "responsiblePerson", "completed", "completionDate", "sortOrder", "taskListTitle", "dueDate", "taskListStatus", "propertyName", "reservationCode", "primaryGuest");
    }

    protected AiToolAnswer taskItemRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
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
}
