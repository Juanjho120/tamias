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

public abstract class AiPurchaseReadSupport extends AiMaintenancePropertyCatalogReadSupport {

    protected AiPurchaseReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> purchaseListRows(String search, List<String> statuses, PurchaseDateRange range, int limit) {
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

    protected List<Map<String, Object>> purchaseItemRows(String search, List<String> flags, PurchaseDateRange range, int limit) {
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

    protected String purchaseCostBaseSql(PurchaseDateRange range, String groupBy, String orderBy) {
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

    protected void setPurchaseCostCommonParams(Query query, PurchaseDateRange range) {
        query.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
        if (range != null) {
            query.setParameter("fromDate", Date.valueOf(range.fromDate()));
            query.setParameter("toDate", Date.valueOf(range.toDate()));
        }
    }

    protected String purchaseItemAggregateSql(String search, String orderMetric, String direction) {
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

    protected PurchaseDateRange purchaseDateRange(String userQuestion) {
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

    protected void appendPurchaseListRows(StringBuilder answer, List<Map<String, Object>> rows) {
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

    protected void appendPurchaseItemRows(StringBuilder answer, List<Map<String, Object>> rows) {
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

    protected void appendDocumentGroups(StringBuilder answer, List<Map<String, Object>> rows, String groupKey) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String groupName = blankToDash(value(row.get(groupKey)));
            grouped.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            answer.append(System.lineSeparator())
                    .append(entry.getKey())
                    .append(" | documentos: ")
                    .append(entry.getValue().size());
            for (Map<String, Object> row : entry.getValue()) {
                answer.append(System.lineSeparator())
                        .append("- ")
                        .append(blankToDash(value(row.get("title"))));
            }
        }
    }


    protected record PurchaseDateRange(LocalDate fromDate, LocalDate toDate, String label) {
    }
}
