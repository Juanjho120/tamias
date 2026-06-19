package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class InventoryToolRepository extends AiReadOnlyToolSupport {

    public InventoryToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer inventorySearch(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
            userQuestion,
            "inventario", "inventory", "item", "items", "producto", "productos", "supply", "supplies", "repuesto", "repuestos", "material", "materiales", "registrado", "registrados"
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
                   ii.available_for_purchases,
                   b.id AS brand_id,
                   b.name AS brand_name
            FROM inventory_items ii
            LEFT JOIN brands b
                   ON b.id = ii.brand_id
                  AND b.organization_id = ii.organization_id
                  AND b.deleted_at IS NULL
            WHERE ii.organization_id = :organizationId
              AND ii.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', ii.name, ii.description, ii.internal_code, ii.barcode, ii.item_type, b.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            ORDER BY ii.status ASC, ii.name ASC, b.name ASC NULLS LAST
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "id", "name", "description", "status", "itemType", "unit", "internalCode", "barcode", "availableForMaintenance", "availableForReservations", "availableForPurchases", "brandId", "brandName");

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

        StringBuilder answer = new StringBuilder(search == null
            ? "Estos son los items de inventario que encontré:"
            : "Encontré estos items relacionados con “" + search + "”:");

        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(itemName(row, "name"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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

    public AiToolAnswer inventoryItemsByBrand(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
            userQuestion,
            "marca", "marcas", "brand", "brands", "items", "item", "inventario", "inventory", "productos", "producto", "de", "del", "la", "el", "por", "con", "que", "qué", "tengo", "hay"
        ));

        List<Map<String, Object>> rows = query("""
            SELECT b.id AS brand_id,
                   b.name AS brand_name,
                   ii.id AS inventory_item_id,
                   ii.name AS item_name,
                   ii.description,
                   ii.status,
                   ii.item_type,
                   ii.unit,
                   ii.available_for_maintenance,
                   ii.available_for_reservations,
                   ii.available_for_purchases,
                   COALESCE(image_count.count_value, 0) AS image_count
            FROM inventory_items ii
            JOIN brands b
              ON b.id = ii.brand_id
             AND b.organization_id = ii.organization_id
             AND b.deleted_at IS NULL
            LEFT JOIN (
                SELECT inventory_item_id, COUNT(*) AS count_value
                FROM inventory_item_images
                WHERE organization_id = :organizationId
                  AND status = 'ACTIVE'
                GROUP BY inventory_item_id
            ) image_count ON image_count.inventory_item_id = ii.id
            WHERE ii.organization_id = :organizationId
              AND ii.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', b.name, ii.name, ii.description, ii.item_type, ii.internal_code, ii.barcode)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            ORDER BY b.name ASC, ii.name ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "brandId", "brandName", "inventoryItemId", "itemName", "description", "status", "itemType", "unit", "availableForMaintenance", "availableForReservations", "availableForPurchases", "imageCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                search == null
                    ? "No encontré items de inventario con marca asignada."
                    : "No encontré items asociados a la marca “" + search + "”.",
                "inventory.getItemsByBrand",
                "Inventory items by brand",
                "No inventory items were found for the requested brand.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
            ? "Estos son los items de inventario agrupados por marca:"
            : "Encontré estos items relacionados con la marca “" + search + "”:");

        if (search == null) {
            appendItemsGroupedByBrand(answer, rows);
        } else {
            for (Map<String, Object> row : rows) {
                answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                    .append(" | unidad: ").append(blankToDash(value(row.get("unit"))));
            }
        }

        return AiToolAnswer.of(
            answer.toString(),
            "inventory.getItemsByBrand",
            "Inventory items by brand",
            "%d inventory items found for brand lookup.".formatted(rows.size()),
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
                   COALESCE(MAX(ii.item_type), 'SNAPSHOT_ONLY') AS item_type,
                   MAX(b.name) AS brand_name
            FROM (
                SELECT rs.inventory_item_id AS inventory_item_id,
                       rs.item_name_snapshot AS item_name,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(rs.quantity), 0) AS total_quantity
                FROM reservation_supplies rs
                WHERE rs.organization_id = :organizationId
                GROUP BY rs.inventory_item_id, rs.item_name_snapshot

                UNION ALL

                SELECT mri.inventory_item_id AS inventory_item_id,
                       mri.item_name_snapshot AS item_name,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(mri.quantity), 0) AS total_quantity
                FROM maintenance_record_items mri
                WHERE mri.organization_id = :organizationId
                GROUP BY mri.inventory_item_id, mri.item_name_snapshot

                UNION ALL

                SELECT pi.inventory_item_id AS inventory_item_id,
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
            LEFT JOIN inventory_items ii
                   ON ii.id = source.inventory_item_id
                  AND ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
            LEFT JOIN brands b
                   ON b.id = ii.brand_id
                  AND b.organization_id = ii.organization_id
                  AND b.deleted_at IS NULL
            GROUP BY source.item_name, source.inventory_item_id
            ORDER BY usage_count DESC, total_quantity DESC, source.item_name ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "inventoryItemId", "usageCount", "totalQuantity", "itemType", "brandName");

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
                .append("- ").append(itemName(row, "itemName"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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
                   ii.available_for_purchases,
                   b.id AS brand_id,
                   b.name AS brand_name
            FROM inventory_items ii
            LEFT JOIN brands b
                   ON b.id = ii.brand_id
                  AND b.organization_id = ii.organization_id
                  AND b.deleted_at IS NULL
            WHERE ii.organization_id = :organizationId
              AND ii.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM reservation_supplies rs
                  WHERE rs.organization_id = ii.organization_id
                    AND rs.inventory_item_id = ii.id
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM maintenance_record_items mri
                  WHERE mri.organization_id = ii.organization_id
                    AND mri.inventory_item_id = ii.id
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM purchase_items pi
                  JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                  WHERE pi.organization_id = ii.organization_id
                    AND pl.organization_id = ii.organization_id
                    AND pl.deleted_at IS NULL
                    AND pi.inventory_item_id = ii.id
              )
            ORDER BY ii.name ASC, b.name ASC NULLS LAST
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "id", "name", "itemType", "unit", "status", "availableForMaintenance", "availableForReservations", "availableForPurchases", "brandId", "brandName");

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
                .append("- ").append(itemName(row, "name"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "he", "se", "ha", "han", "usado", "usados", "usaron", "usa", "usan", "uso", "reservacion", "reservaciones", "reserva", "reservas", "supply", "supplies", "item", "items"));

        List<Map<String, Object>> rows = query("""
            SELECT rs.item_name_snapshot,
                   COUNT(*) AS usage_count,
                   COALESCE(SUM(rs.quantity), 0) AS total_quantity,
                   COALESCE(MAX(rs.unit), '') AS unit,
                   MAX(r.check_in) AS last_check_in,
                   COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties,
                   MAX(b.name) AS brand_name
            FROM reservation_supplies rs
            JOIN reservations r
              ON r.id = rs.reservation_id
             AND r.organization_id = rs.organization_id
            JOIN properties p
              ON p.id = r.property_id
             AND p.organization_id = rs.organization_id
            LEFT JOIN inventory_items ii
              ON ii.id = rs.inventory_item_id
             AND ii.organization_id = rs.organization_id
             AND ii.deleted_at IS NULL
            LEFT JOIN brands b
              ON b.id = ii.brand_id
             AND b.organization_id = ii.organization_id
             AND b.deleted_at IS NULL
            WHERE rs.organization_id = :organizationId
              AND r.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', rs.item_name_snapshot, rs.notes, b.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY rs.item_name_snapshot
            ORDER BY usage_count DESC, last_check_in DESC NULLS LAST, rs.item_name_snapshot ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "usageCount", "totalQuantity", "unit", "lastCheckIn", "properties", "brandName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                search == null ? "No encontré supplies registrados en reservaciones." : "No encontré uso en reservaciones para “" + search + "”.",
                "inventory.getItemsUsedInReservations",
                "Inventory items used in reservations",
                "No reservation supply usage found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos supplies aparecen en reservaciones:" : "Encontré este uso en reservaciones para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(itemName(row, "itemName"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "he", "se", "ha", "han", "usado", "usados", "uso", "comprado", "compre", "compras", "compra", "historial", "item", "items", "producto", "productos"));

        List<Map<String, Object>> rows = query("""
            SELECT pi.item_name_snapshot,
                   COUNT(*) AS purchase_count,
                   COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                   COALESCE(MAX(pi.unit), '') AS unit,
                   COALESCE(SUM(pi.estimated_price), 0) AS total_estimated_cost,
                   MAX(pl.purchase_date) AS last_purchase_date,
                   COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties,
                   MAX(b.name) AS brand_name
            FROM purchase_items pi
            JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
            LEFT JOIN properties p ON p.id = pl.property_id
            LEFT JOIN inventory_items ii
              ON ii.id = pi.inventory_item_id
             AND ii.organization_id = pi.organization_id
             AND ii.deleted_at IS NULL
            LEFT JOIN brands b
              ON b.id = ii.brand_id
             AND b.organization_id = ii.organization_id
             AND b.deleted_at IS NULL
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
                          AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes, b.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY pi.item_name_snapshot
            ORDER BY last_purchase_date DESC NULLS LAST, purchase_count DESC, pi.item_name_snapshot ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "purchaseCount", "totalQuantity", "unit", "totalEstimatedCost", "lastPurchaseDate", "properties", "brandName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                search == null ? "No encontré items marcados como comprados." : "No encontré compras marcadas como compradas para “" + search + "”.",
                "inventory.getItemsUsedInPurchases",
                "Inventory items used in purchases",
                "No purchased item usage found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos items aparecen en compras marcadas como compradas:" : "Encontré compras para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(itemName(row, "itemName"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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
        String search = nullableSearch(extractSearchText(userQuestion, "donde", "he", "se", "ha", "han", "usado", "usados", "usaron", "usa", "usan", "uso", "mantenimiento", "mantenimientos", "item", "items", "repuesto", "repuestos", "material", "materiales"));

        List<Map<String, Object>> rows = query("""
            SELECT mri.item_name_snapshot,
                   COUNT(*) AS usage_count,
                   COALESCE(SUM(mri.quantity), 0) AS total_quantity,
                   COALESCE(MAX(mri.unit), '') AS unit,
                   MAX(COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)) AS last_used_at,
                   COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties,
                   MAX(b.name) AS brand_name
            FROM maintenance_record_items mri
            JOIN maintenance_records mr
              ON mr.id = mri.maintenance_record_id
             AND mr.organization_id = mri.organization_id
            JOIN properties p
              ON p.id = mr.property_id
             AND p.organization_id = mri.organization_id
            LEFT JOIN inventory_items ii
              ON ii.id = mri.inventory_item_id
             AND ii.organization_id = mri.organization_id
             AND ii.deleted_at IS NULL
            LEFT JOIN brands b
              ON b.id = ii.brand_id
             AND b.organization_id = ii.organization_id
             AND b.deleted_at IS NULL
            WHERE mri.organization_id = :organizationId
              AND mr.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', mri.item_name_snapshot, mri.notes, mr.title, mr.description, b.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY mri.item_name_snapshot
            ORDER BY last_used_at DESC NULLS LAST, usage_count DESC, mri.item_name_snapshot ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "itemName", "usageCount", "totalQuantity", "unit", "lastUsedAt", "properties", "brandName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                search == null ? "No encontré items usados en mantenimientos." : "No encontré uso en mantenimientos para “" + search + "”.",
                "inventory.getItemsUsedInMaintenance",
                "Inventory items used in maintenance",
                "No maintenance item usage found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null ? "Estos items aparecen en mantenimientos:" : "Encontré uso en mantenimientos para “" + search + "”:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(itemName(row, "itemName"))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
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

    private void appendItemsGroupedByBrand(StringBuilder answer, List<Map<String, Object>> rows) {
        String currentBrand = null;
        for (Map<String, Object> row : rows) {
            String brandName = blankToDash(value(row.get("brandName")));
            if (!brandName.equals(currentBrand)) {
                if (currentBrand != null) {
                    answer.append(System.lineSeparator());
                }
                answer.append(System.lineSeparator()).append(brandName);
                currentBrand = brandName;
            }
            answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("itemName"))))
                .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                .append(" | unidad: ").append(blankToDash(value(row.get("unit"))));
        }
    }

    private String itemName(Map<String, Object> row, String itemNameKey) {
        return blankToDash(value(row.get(itemNameKey)));
    }
}
