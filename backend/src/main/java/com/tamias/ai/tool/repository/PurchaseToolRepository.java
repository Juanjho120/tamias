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
public class PurchaseToolRepository extends AiReadOnlyToolSupport {

    public PurchaseToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
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
}
