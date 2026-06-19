package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class EntityImageToolRepository extends AiReadOnlyToolSupport {

    public EntityImageToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer reservationImages(String userQuestion, boolean withoutImages) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
            userQuestion,
            "imagen", "imagenes", "imágenes", "image", "images", "foto", "fotos",
            "reservacion", "reservaciones", "reservación", "reservaciónes", "reserva", "reservas",
            "tiene", "tienen", "con", "sin", "de", "del", "la", "el", "que", "qué", "cuales", "cuáles"
        ));

        List<Map<String, Object>> rows = query("""
            SELECT r.id AS reservation_id,
                   COALESCE(r.reservation_code, '') AS reservation_code,
                   p.name AS property_name,
                   COALESCE(platform.name, '') AS platform_name,
                   r.check_in,
                   r.check_out,
                   r.status,
                   COALESCE(STRING_AGG(DISTINCT g.full_name, ', '), '') AS guests,
                   COUNT(ri.id) AS image_count,
                   COALESCE(STRING_AGG(DISTINCT ri.original_filename, ', '), '') AS filenames,
                   MAX(ri.created_at) AS last_image_at
            FROM reservations r
            JOIN properties p
              ON p.id = r.property_id
             AND p.organization_id = r.organization_id
            LEFT JOIN platforms platform
              ON platform.id = r.platform_id
             AND platform.organization_id = r.organization_id
             AND platform.deleted_at IS NULL
            LEFT JOIN reservation_guests rg
              ON rg.reservation_id = r.id
             AND rg.organization_id = r.organization_id
            LEFT JOIN guests g
              ON g.id = rg.guest_id
             AND g.organization_id = r.organization_id
             AND g.deleted_at IS NULL
            LEFT JOIN reservation_images ri
              ON ri.reservation_id = r.id
             AND ri.organization_id = r.organization_id
             AND ri.status = 'ACTIVE'
            WHERE r.organization_id = :organizationId
              AND r.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', r.reservation_code, p.name, platform.name, g.full_name, ri.original_filename)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY r.id, r.reservation_code, p.name, platform.name, r.check_in, r.check_out, r.status
            HAVING (:withoutImages = TRUE AND COUNT(ri.id) = 0)
                OR (:withoutImages = FALSE AND COUNT(ri.id) > 0)
            ORDER BY image_count DESC, last_image_at DESC NULLS LAST, r.check_in DESC, p.name ASC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("withoutImages", withoutImages);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "reservationId", "reservationCode", "propertyName", "platformName", "checkIn", "checkOut", "status", "guests", "imageCount", "filenames", "lastImageAt");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                withoutImages
                    ? "No encontré reservaciones sin imágenes con esos criterios."
                    : "No encontré imágenes de reservaciones con esos criterios.",
                "images.getReservationImages",
                "Reservation images",
                "No reservation image rows found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(withoutImages
            ? "Estas reservaciones no tienen imágenes registradas:"
            : "Estas reservaciones tienen imágenes registradas:");

        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("propertyName"))))
                .append(" | código: ").append(blankToDash(value(row.get("reservationCode"))))
                .append(" | check-in: ").append(blankToDash(value(row.get("checkIn"))))
                .append(" | check-out: ").append(blankToDash(value(row.get("checkOut"))))
                .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
            appendOptionalFilenames(answer, row);
        }

        return AiToolAnswer.of(
            answer.toString(),
            "images.getReservationImages",
            "Reservation images",
            "%d reservation image rows found.".formatted(rows.size()),
            rows
        );
    }

    public AiToolAnswer inventoryItemImages(String userQuestion, boolean withoutImages) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
            userQuestion,
            "imagen", "imagenes", "imágenes", "image", "images", "foto", "fotos",
            "item", "items", "inventario", "inventory", "producto", "productos", "catalogo", "catálogo",
            "tiene", "tienen", "con", "sin", "de", "del", "la", "el", "que", "qué", "cuales", "cuáles"
        ));

        List<Map<String, Object>> rows = query("""
            SELECT ii.id AS inventory_item_id,
                   ii.name AS item_name,
                   b.name AS brand_name,
                   ii.item_type,
                   ii.unit,
                   ii.status,
                   COUNT(iii.id) AS image_count,
                   COALESCE(STRING_AGG(DISTINCT iii.original_filename, ', '), '') AS filenames,
                   MAX(iii.created_at) AS last_image_at
            FROM inventory_items ii
            LEFT JOIN brands b
              ON b.id = ii.brand_id
             AND b.organization_id = ii.organization_id
             AND b.deleted_at IS NULL
            LEFT JOIN inventory_item_images iii
              ON iii.inventory_item_id = ii.id
             AND iii.organization_id = ii.organization_id
             AND iii.status = 'ACTIVE'
            WHERE ii.organization_id = :organizationId
              AND ii.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', ii.name, ii.description, ii.item_type, ii.internal_code, ii.barcode, b.name, iii.original_filename)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY ii.id, ii.name, b.name, ii.item_type, ii.unit, ii.status
            HAVING (:withoutImages = TRUE AND COUNT(iii.id) = 0)
                OR (:withoutImages = FALSE AND COUNT(iii.id) > 0)
            ORDER BY image_count DESC, last_image_at DESC NULLS LAST, ii.name ASC, b.name ASC NULLS LAST
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("withoutImages", withoutImages);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "inventoryItemId", "itemName", "brandName", "itemType", "unit", "status", "imageCount", "filenames", "lastImageAt");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                withoutImages
                    ? "No encontré items sin imágenes con esos criterios."
                    : "No encontré imágenes de items con esos criterios.",
                "images.getInventoryItemImages",
                "Inventory item images",
                "No inventory item image rows found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(withoutImages
            ? "Estos items no tienen imágenes registradas:"
            : "Estos items tienen imágenes registradas:");

        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("itemName"))))
                .append(" | marca: ").append(blankToDash(value(row.get("brandName"))))
                .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
            appendOptionalFilenames(answer, row);
        }

        return AiToolAnswer.of(
            answer.toString(),
            "images.getInventoryItemImages",
            "Inventory item images",
            "%d inventory item image rows found.".formatted(rows.size()),
            rows
        );
    }

    public AiToolAnswer purchaseImages(String userQuestion, boolean withoutImages) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
            userQuestion,
            "imagen", "imagenes", "imágenes", "image", "images", "foto", "fotos",
            "compra", "compras", "purchase", "purchases", "lista", "listas", "listado", "listados",
            "tiene", "tienen", "con", "sin", "de", "del", "la", "el", "que", "qué", "cuales", "cuáles"
        ));

        List<Map<String, Object>> rows = query("""
            SELECT pl.id AS purchase_list_id,
                   pl.purchase_date,
                   pl.status,
                   COALESCE(p.name, '') AS property_name,
                   COALESCE(s.name, '') AS supplier_name,
                   COALESCE(c.name, '') AS city_name,
                   COUNT(pi.id) AS image_count,
                   COALESCE(STRING_AGG(DISTINCT pi.original_filename, ', '), '') AS filenames,
                   MAX(pi.created_at) AS last_image_at
            FROM purchase_lists pl
            LEFT JOIN properties p
              ON p.id = pl.property_id
             AND p.organization_id = pl.organization_id
            LEFT JOIN suppliers s
              ON s.id = pl.supplier_id
             AND s.organization_id = pl.organization_id
             AND s.deleted_at IS NULL
            LEFT JOIN cities c
              ON c.id = pl.city_id
             AND c.organization_id = pl.organization_id
             AND c.deleted_at IS NULL
            LEFT JOIN purchase_images pi
              ON pi.purchase_list_id = pl.id
             AND pi.organization_id = pl.organization_id
             AND pi.status = 'ACTIVE'
            WHERE pl.organization_id = :organizationId
              AND pl.deleted_at IS NULL
              AND (
                   CAST(:search AS TEXT) IS NULL
                   OR NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', pl.notes, pl.status, p.name, s.name, c.name, pi.original_filename, CAST(pl.purchase_date AS TEXT))), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                   )
              )
            GROUP BY pl.id, pl.purchase_date, pl.status, p.name, s.name, c.name
            HAVING (:withoutImages = TRUE AND COUNT(pi.id) = 0)
                OR (:withoutImages = FALSE AND COUNT(pi.id) > 0)
            ORDER BY image_count DESC, last_image_at DESC NULLS LAST, pl.purchase_date DESC
            LIMIT :limit
            """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("search", search);
            q.setParameter("withoutImages", withoutImages);
            q.setParameter("limit", DEFAULT_LIMIT);
        }, "purchaseListId", "purchaseDate", "status", "propertyName", "supplierName", "cityName", "imageCount", "filenames", "lastImageAt");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                withoutImages
                    ? "No encontré listas de compra sin imágenes con esos criterios."
                    : "No encontré imágenes de listas de compra con esos criterios.",
                "images.getPurchaseListImages",
                "Purchase list images",
                "No purchase image rows found.",
                List.of()
            );
        }

        StringBuilder answer = new StringBuilder(withoutImages
            ? "Estas listas de compra no tienen imágenes registradas:"
            : "Estas listas de compra tienen imágenes registradas:");

        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                .append("- fecha: ").append(blankToDash(value(row.get("purchaseDate"))))
                .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))))
                .append(" | ciudad: ").append(blankToDash(value(row.get("cityName"))))
                .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
            appendOptionalFilenames(answer, row);
        }

        return AiToolAnswer.of(
            answer.toString(),
            "images.getPurchaseListImages",
            "Purchase list images",
            "%d purchase image rows found.".formatted(rows.size()),
            rows
        );
    }

    private void appendOptionalFilenames(StringBuilder answer, Map<String, Object> row) {
        String filenames = value(row.get("filenames"));
        if (filenames != null && !filenames.isBlank()) {
            answer.append(" | archivos: ").append(filenames);
        }
    }
}
