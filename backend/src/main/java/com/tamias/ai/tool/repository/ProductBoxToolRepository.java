package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProductBoxToolRepository extends AiReadOnlyToolSupport {

    private static final int EXPECTED_FACE_COUNT = 6;

    public ProductBoxToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer productBoxSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        List<Map<String, Object>> rows = query(
                """
                WITH model_stats AS (
                    SELECT
                        pbm.id,
                        pbm.inventory_item_id,
                        pbm.purchase_item_id,
                        COUNT(DISTINCT CASE
                            WHEN COALESCE(f.s3_key, f.original_s3_key, f.processed_s3_key, f.ai_enhanced_s3_key) IS NOT NULL
                            THEN f.face_name
                        END) AS active_face_count,
                        COUNT(DISTINCT CASE WHEN f.original_s3_key IS NOT NULL THEN f.face_name END) AS original_texture_count,
                        COUNT(DISTINCT CASE WHEN f.processed_s3_key IS NOT NULL THEN f.face_name END) AS processed_texture_count,
                        COUNT(DISTINCT CASE WHEN f.ai_enhanced_s3_key IS NOT NULL THEN f.face_name END) AS ai_enhanced_texture_count
                    FROM product_box_models pbm
                    LEFT JOIN product_box_model_faces f
                        ON f.product_box_model_id = pbm.id
                       AND f.organization_id = pbm.organization_id
                    WHERE pbm.organization_id = :organizationId
                      AND pbm.deleted_at IS NULL
                    GROUP BY pbm.id, pbm.inventory_item_id, pbm.purchase_item_id
                )
                SELECT
                    COUNT(*) AS total_models,
                    COALESCE(SUM(CASE WHEN inventory_item_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS inventory_linked_models,
                    COALESCE(SUM(CASE WHEN purchase_item_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS purchase_linked_models,
                    COALESCE(SUM(CASE WHEN active_face_count >= 6 THEN 1 ELSE 0 END), 0) AS complete_models,
                    COALESCE(SUM(CASE WHEN active_face_count < 6 THEN 1 ELSE 0 END), 0) AS incomplete_models,
                    COALESCE(SUM(active_face_count), 0) AS active_texture_faces,
                    COALESCE(SUM(original_texture_count), 0) AS original_texture_faces,
                    COALESCE(SUM(processed_texture_count), 0) AS processed_texture_faces,
                    COALESCE(SUM(ai_enhanced_texture_count), 0) AS ai_enhanced_texture_faces
                FROM model_stats
                """,
                q -> q.setParameter("organizationId", organizationId),
                "totalModels",
                "inventoryLinkedModels",
                "purchaseLinkedModels",
                "completeModels",
                "incompleteModels",
                "activeTextureFaces",
                "originalTextureFaces",
                "processedTextureFaces",
                "aiEnhancedTextureFaces"
        );

        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        if (longValue(row.get("totalModels")) == 0) {
            return AiToolAnswer.of(
                    "Todavía no hay modelos Product Box registrados.",
                    "productBox.summary",
                    "Product Box summary",
                    "No Product Box models found.",
                    List.of()
            );
        }

        String answer = "Resumen de Product Box Models:" + System.lineSeparator()
                + "- Modelos registrados: " + value(row.get("totalModels")) + System.lineSeparator()
                + "- Asociados a inventario: " + value(row.get("inventoryLinkedModels")) + System.lineSeparator()
                + "- Asociados a compras: " + value(row.get("purchaseLinkedModels")) + System.lineSeparator()
                + "- Modelos completos: " + value(row.get("completeModels")) + System.lineSeparator()
                + "- Modelos incompletos: " + value(row.get("incompleteModels")) + System.lineSeparator()
                + "- Caras con textura activa: " + value(row.get("activeTextureFaces")) + System.lineSeparator()
                + "- Caras con textura original: " + value(row.get("originalTextureFaces")) + System.lineSeparator()
                + "- Caras con textura procesada: " + value(row.get("processedTextureFaces")) + System.lineSeparator()
                + "- Caras con textura AI-enhanced: " + value(row.get("aiEnhancedTextureFaces"));

        return AiToolAnswer.of(
                answer,
                "productBox.summary",
                "Product Box summary",
                "Product Box aggregate metrics found.",
                rows
        );
    }

    public AiToolAnswer productBoxSearch(String userQuestion) {
        String search = extractProductBoxSearchText(userQuestion);
        StringBuilder sql = new StringBuilder(modelStatsSql());
        sql.append(
                """
                SELECT
                    model_id,
                    model_name,
                    description,
                    width,
                    height,
                    depth,
                    unit,
                    inventory_item_id,
                    inventory_item_name,
                    inventory_item_brand_name,
                    inventory_item_type,
                    purchase_item_id,
                    purchase_item_name,
                    purchase_item_brand_name,
                    purchase_inventory_item_name,
                    purchase_inventory_item_brand_name,
                    purchase_item_purchased,
                    active_face_count,
                    (6 - active_face_count) AS missing_face_count,
                    original_texture_count,
                    processed_texture_count,
                    ai_enhanced_texture_count,
                    texture_statuses,
                    ai_statuses,
                    active_texture_sources
                FROM model_stats
                WHERE 1 = 1
                """
        );
        if (search != null) {
            sql.append(
                    """
                    AND translate(LOWER(CONCAT_WS(' ',
                        model_name,
                        description,
                        inventory_item_name,
                        inventory_item_brand_name,
                        purchase_item_name,
                        purchase_item_brand_name,
                        purchase_inventory_item_name,
                        purchase_inventory_item_brand_name,
                        CAST(model_id AS TEXT),
                        CAST(purchase_item_id AS TEXT)
                    )), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', :search, '%')
                    """
            );
        }
        sql.append(" ORDER BY updated_at DESC NULLS LAST, model_name ASC LIMIT :limit ");

        List<Map<String, Object>> rows = query(
                sql.toString(),
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    if (search != null) {
                        q.setParameter("search", search);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                modelAliases()
        );

        String emptyMessage = search == null
                ? "No encontré modelos Product Box registrados."
                : "No encontré modelos Product Box que coincidan con “" + search + "”.";

        return productBoxRowsAnswer(
                rows,
                "productBox.search",
                "Product Box search",
                search == null
                        ? "Estos son los modelos Product Box más recientes:"
                        : "Encontré estos modelos Product Box relacionados con “" + search + "”:",
                emptyMessage
        );
    }

    public AiToolAnswer productBoxIncompleteModels() {
        String sql = missingFacesSql() +
                """
                SELECT
                    ms.model_id,
                    ms.model_name,
                    ms.description,
                    ms.width,
                    ms.height,
                    ms.depth,
                    ms.unit,
                    ms.inventory_item_id,
                    ms.inventory_item_name,
                    ms.inventory_item_brand_name,
                    ms.inventory_item_type,
                    ms.purchase_item_id,
                    ms.purchase_item_name,
                    ms.purchase_item_brand_name,
                    ms.purchase_inventory_item_name,
                    ms.purchase_inventory_item_brand_name,
                    ms.purchase_item_purchased,
                    ms.active_face_count,
                    (6 - ms.active_face_count) AS missing_face_count,
                    COALESCE(mf.missing_faces, '') AS missing_faces,
                    ms.original_texture_count,
                    ms.processed_texture_count,
                    ms.ai_enhanced_texture_count,
                    ms.texture_statuses,
                    ms.ai_statuses,
                    ms.active_texture_sources
                FROM model_stats ms
                LEFT JOIN missing_faces mf ON mf.model_id = ms.model_id
                WHERE ms.active_face_count < 6
                ORDER BY missing_face_count DESC, ms.updated_at DESC NULLS LAST, ms.model_name ASC
                LIMIT :limit
                """;

        List<Map<String, Object>> rows = query(
                sql,
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                "modelId",
                "modelName",
                "description",
                "width",
                "height",
                "depth",
                "unit",
                "inventoryItemId",
                "inventoryItemName",
                "inventoryItemBrandName",
                "inventoryItemType",
                "purchaseItemId",
                "purchaseItemName",
                "purchaseItemBrandName",
                "purchaseInventoryItemName",
                "purchaseInventoryItemBrandName",
                "purchaseItemPurchased",
                "activeFaceCount",
                "missingFaceCount",
                "missingFaces",
                "originalTextureCount",
                "processedTextureCount",
                "aiEnhancedTextureCount",
                "textureStatuses",
                "aiStatuses",
                "activeTextureSources"
        );

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré modelos Product Box incompletos.\nTodos los modelos registrados tienen sus 6 caras con textura activa.",
                    "productBox.incompleteModels",
                    "Incomplete Product Box models",
                    "No incomplete Product Box models found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos modelos Product Box necesitan atención:");
        for (Map<String, Object> row : rows) {
            appendProductBoxRow(answer, row);
            answer.append(" | faltan: ").append(blankToDash(value(row.get("missingFaces"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "productBox.incompleteModels",
                "Incomplete Product Box models",
                "%d incomplete Product Box models found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer productBoxInventoryLinks(String userQuestion) {
        String search = extractProductBoxSearchText(userQuestion);
        StringBuilder sql = new StringBuilder(modelStatsSql());
        sql.append(
                """
                SELECT
                    model_id,
                    model_name,
                    description,
                    width,
                    height,
                    depth,
                    unit,
                    inventory_item_id,
                    inventory_item_name,
                    inventory_item_brand_name,
                    inventory_item_type,
                    purchase_item_id,
                    purchase_item_name,
                    purchase_item_brand_name,
                    purchase_inventory_item_name,
                    purchase_inventory_item_brand_name,
                    purchase_item_purchased,
                    active_face_count,
                    (6 - active_face_count) AS missing_face_count,
                    original_texture_count,
                    processed_texture_count,
                    ai_enhanced_texture_count,
                    texture_statuses,
                    ai_statuses,
                    active_texture_sources
                FROM model_stats
                WHERE inventory_item_id IS NOT NULL
                """
        );
        if (search != null) {
            sql.append(
                    """
                    AND translate(LOWER(CONCAT_WS(' ',
                        model_name,
                        inventory_item_name,
                        inventory_item_brand_name,
                        inventory_item_type,
                        description
                    )), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', :search, '%')
                    """
            );
        }
        sql.append(" ORDER BY inventory_item_brand_name ASC NULLS LAST, inventory_item_name ASC, model_name ASC LIMIT :limit ");

        List<Map<String, Object>> rows = query(
                sql.toString(),
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    if (search != null) {
                        q.setParameter("search", search);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                modelAliases()
        );

        return productBoxInventoryRowsAnswer(
                rows,
                "productBox.inventoryLinks",
                "Product Box inventory links",
                "Los modelos Product Box están asociados a los siguientes items de inventario:",
                "No encontré modelos Product Box asociados a items de inventario."
        );
    }

    public AiToolAnswer inventoryItemsWithoutProductBox(String userQuestion) {
        String search = extractProductBoxSearchText(userQuestion);
        StringBuilder sql = new StringBuilder(
                """
                SELECT
                    ii.id AS inventory_item_id,
                    ii.name AS inventory_item_name,
                    b.name AS inventory_item_brand_name,
                    COALESCE(ii.description, '') AS description,
                    ii.item_type,
                    COALESCE(ii.unit, '') AS unit,
                    ii.available_for_purchases,
                    ii.available_for_maintenance,
                    ii.available_for_reservations
                FROM inventory_items ii
                LEFT JOIN brands b
                    ON b.id = ii.brand_id
                   AND b.organization_id = ii.organization_id
                   AND b.deleted_at IS NULL
                WHERE ii.organization_id = :organizationId
                  AND ii.deleted_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM product_box_models pbm
                      WHERE pbm.organization_id = ii.organization_id
                        AND pbm.inventory_item_id = ii.id
                        AND pbm.deleted_at IS NULL
                  )
                """
        );
        if (search != null) {
            sql.append(
                    """
                    AND translate(LOWER(CONCAT_WS(' ', ii.name, b.name, ii.description, ii.item_type, ii.unit)), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', :search, '%')
                    """
            );
        }
        sql.append(" ORDER BY b.name ASC NULLS LAST, ii.name ASC LIMIT :limit ");

        List<Map<String, Object>> rows = query(
                sql.toString(),
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    if (search != null) {
                        q.setParameter("search", search);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                "inventoryItemId",
                "inventoryItemName",
                "inventoryItemBrandName",
                "description",
                "itemType",
                "unit",
                "availableForPurchases",
                "availableForMaintenance",
                "availableForReservations"
        );

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré items de inventario activos sin Product Box Model.",
                    "productBox.inventoryItemsWithoutModel",
                    "Inventory items without Product Box",
                    "No inventory items without Product Box were found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos items de inventario aún no tienen Product Box Model:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(brandedItemName(row, "inventoryItemName", "inventoryItemBrandName"))
                    .append(" | tipo: ").append(blankToDash(value(row.get("itemType"))))
                    .append(" | unidad: ").append(blankToDash(value(row.get("unit"))));
        }

        return AiToolAnswer.of(
                answer.toString(),
                "productBox.inventoryItemsWithoutModel",
                "Inventory items without Product Box",
                "%d inventory items without Product Box found.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer productBoxPurchaseLinks(String userQuestion) {
        String search = extractProductBoxSearchText(userQuestion);
        StringBuilder sql = new StringBuilder(modelStatsSql());
        sql.append(
                """
                SELECT
                    model_id,
                    model_name,
                    description,
                    width,
                    height,
                    depth,
                    unit,
                    inventory_item_id,
                    inventory_item_name,
                    inventory_item_brand_name,
                    inventory_item_type,
                    purchase_item_id,
                    purchase_item_name,
                    purchase_item_brand_name,
                    purchase_inventory_item_name,
                    purchase_inventory_item_brand_name,
                    purchase_item_purchased,
                    active_face_count,
                    (6 - active_face_count) AS missing_face_count,
                    original_texture_count,
                    processed_texture_count,
                    ai_enhanced_texture_count,
                    texture_statuses,
                    ai_statuses,
                    active_texture_sources
                FROM model_stats
                WHERE purchase_item_id IS NOT NULL
                """
        );
        if (search != null) {
            sql.append(
                    """
                    AND translate(LOWER(CONCAT_WS(' ',
                        model_name,
                        purchase_item_name,
                        purchase_item_brand_name,
                        inventory_item_name,
                        inventory_item_brand_name,
                        purchase_inventory_item_name,
                        purchase_inventory_item_brand_name,
                        description,
                        CAST(purchase_item_id AS TEXT)
                    )), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', :search, '%')
                    """
            );
        }
        sql.append(" ORDER BY purchase_item_brand_name ASC NULLS LAST, purchase_item_name ASC NULLS LAST, model_name ASC LIMIT :limit ");

        List<Map<String, Object>> rows = query(
                sql.toString(),
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    if (search != null) {
                        q.setParameter("search", search);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                modelAliases()
        );

        return productBoxPurchaseRowsAnswer(
                rows,
                "productBox.purchaseLinks",
                "Product Box purchase links",
                "Los modelos Product Box están asociados a los siguientes items de compra:",
                "No encontré modelos Product Box asociados a items de compra."
        );
    }

    public AiToolAnswer productBoxTextureStatus(String userQuestion) {
        String search = extractProductBoxSearchText(userQuestion);
        StringBuilder sql = new StringBuilder(modelStatsSql());
        sql.append(
                """
                SELECT
                    model_id,
                    model_name,
                    description,
                    width,
                    height,
                    depth,
                    unit,
                    inventory_item_id,
                    inventory_item_name,
                    inventory_item_brand_name,
                    inventory_item_type,
                    purchase_item_id,
                    purchase_item_name,
                    purchase_item_brand_name,
                    purchase_inventory_item_name,
                    purchase_inventory_item_brand_name,
                    purchase_item_purchased,
                    active_face_count,
                    (6 - active_face_count) AS missing_face_count,
                    original_texture_count,
                    processed_texture_count,
                    ai_enhanced_texture_count,
                    texture_statuses,
                    ai_statuses,
                    active_texture_sources
                FROM model_stats
                WHERE original_texture_count > 0
                   OR processed_texture_count > 0
                   OR ai_enhanced_texture_count > 0
                   OR active_face_count > 0
                """
        );
        if (search != null) {
            sql.append(
                    """
                    AND translate(LOWER(CONCAT_WS(' ',
                        model_name,
                        inventory_item_name,
                        inventory_item_brand_name,
                        purchase_item_name,
                        purchase_item_brand_name,
                        purchase_inventory_item_name,
                        purchase_inventory_item_brand_name,
                        texture_statuses,
                        ai_statuses,
                        active_texture_sources
                    )), 'áéíóúüñ', 'aeiouun') LIKE CONCAT('%', :search, '%')
                    """
            );
        }
        sql.append(" ORDER BY ai_enhanced_texture_count DESC, processed_texture_count DESC, original_texture_count DESC, model_name ASC LIMIT :limit ");

        List<Map<String, Object>> rows = query(
                sql.toString(),
                q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    if (search != null) {
                        q.setParameter("search", search);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                },
                modelAliases()
        );

        return productBoxRowsAnswer(
                rows,
                "productBox.textureStatus",
                "Product Box texture status",
                "Este es el estado de texturas de los modelos Product Box:",
                "No encontré texturas registradas en Product Box Models."
        );
    }

    private AiToolAnswer productBoxRowsAnswer(
            List<Map<String, Object>> rows,
            String toolName,
            String label,
            String intro,
            String emptyMessage
    ) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, label, "No Product Box rows found.", List.of());
        }

        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            appendProductBoxRow(answer, row);
        }

        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d Product Box rows found.".formatted(rows.size()),
                rows
        );
    }

    private AiToolAnswer productBoxInventoryRowsAnswer(
            List<Map<String, Object>> rows,
            String toolName,
            String label,
            String intro,
            String emptyMessage
    ) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, label, "No Product Box inventory links found.", List.of());
        }

        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            appendInventoryLinkRow(answer, row);
        }

        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d Product Box inventory links found.".formatted(rows.size()),
                rows
        );
    }

    private AiToolAnswer productBoxPurchaseRowsAnswer(
            List<Map<String, Object>> rows,
            String toolName,
            String label,
            String intro,
            String emptyMessage
    ) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, label, "No Product Box purchase links found.", List.of());
        }

        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            appendPurchaseLinkRow(answer, row);
        }

        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d Product Box purchase links found.".formatted(rows.size()),
                rows
        );
    }

    private void appendProductBoxRow(StringBuilder answer, Map<String, Object> row) {
        answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("modelName"))))
                .append(" | medidas: ").append(dimensions(row))
                .append(" | caras activas: ").append(blankToDash(value(row.get("activeFaceCount"))))
                .append("/").append(EXPECTED_FACE_COUNT)
                .append(" | faltantes: ").append(blankToDash(value(row.get("missingFaceCount"))))
                .append(" | original/procesada/AI: ")
                .append(blankToDash(value(row.get("originalTextureCount"))))
                .append("/")
                .append(blankToDash(value(row.get("processedTextureCount"))))
                .append("/")
                .append(blankToDash(value(row.get("aiEnhancedTextureCount"))));

        String inventoryName = brandedItemName(row, "inventoryItemName", "inventoryItemBrandName");
        if (!inventoryName.isBlank()) {
            answer.append(" | inventario: ").append(inventoryName);
        }

        String purchaseName = purchaseItemDisplayName(row);
        if (!purchaseName.isBlank()) {
            answer.append(" | compra: ").append(purchaseName);
        }

        String textureStatuses = value(row.get("textureStatuses"));
        if (!textureStatuses.isBlank()) {
            answer.append(" | estados textura: ").append(textureStatuses);
        }

        String aiStatuses = value(row.get("aiStatuses"));
        if (!aiStatuses.isBlank()) {
            answer.append(" | estados AI: ").append(aiStatuses);
        }
    }

    private void appendInventoryLinkRow(StringBuilder answer, Map<String, Object> row) {
        answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("modelName"))))
                .append(" | inventario: ").append(blankToDash(brandedItemName(row, "inventoryItemName", "inventoryItemBrandName")));
    }

    private void appendPurchaseLinkRow(StringBuilder answer, Map<String, Object> row) {
        String purchaseName = purchaseItemDisplayName(row);
        if (purchaseName.isBlank()) {
            purchaseName = value(row.get("purchaseItemId"));
        }

        answer.append(System.lineSeparator())
                .append("- ").append(blankToDash(value(row.get("modelName"))))
                .append(" | compra: ").append(blankToDash(purchaseName));

        String inventoryName = purchaseInventoryDisplayName(row);
        if (!inventoryName.isBlank()) {
            answer.append(" | inventario: ").append(inventoryName);
        }
    }

    private String purchaseItemDisplayName(Map<String, Object> row) {
        return brandedName(value(row.get("purchaseItemName")), value(row.get("purchaseItemBrandName")));
    }

    private String purchaseInventoryDisplayName(Map<String, Object> row) {
        String purchaseInventoryName = brandedName(
                value(row.get("purchaseInventoryItemName")),
                value(row.get("purchaseInventoryItemBrandName"))
        );
        if (!purchaseInventoryName.isBlank()) {
            return purchaseInventoryName;
        }
        return brandedItemName(row, "inventoryItemName", "inventoryItemBrandName");
    }

    private String brandedItemName(Map<String, Object> row, String itemNameKey, String brandNameKey) {
        return brandedName(value(row.get(itemNameKey)), value(row.get(brandNameKey)));
    }

    private String brandedName(String itemName, String brandName) {
        String item = value(itemName).trim();
        String brand = value(brandName).trim();

        if (item.isBlank()) {
            return "";
        }
        if (brand.isBlank()) {
            return item;
        }
        if (normalizeForSearch(item).contains(normalizeForSearch(brand))) {
            return item;
        }
        return brand + " " + item;
    }

    private String dimensions(Map<String, Object> row) {
        return blankToDash(value(row.get("width")))
                + " x "
                + blankToDash(value(row.get("height")))
                + " x "
                + blankToDash(value(row.get("depth")))
                + " "
                + blankToDash(value(row.get("unit")));
    }

    private String modelStatsSql() {
        return """
                WITH model_stats AS (
                    SELECT
                        pbm.id AS model_id,
                        pbm.name AS model_name,
                        COALESCE(pbm.description, '') AS description,
                        pbm.width,
                        pbm.height,
                        pbm.depth,
                        pbm.unit,
                        pbm.inventory_item_id,
                        ii.name AS inventory_item_name,
                        ib.name AS inventory_item_brand_name,
                        ii.item_type AS inventory_item_type,
                        pbm.purchase_item_id,
                        pi.item_name_snapshot AS purchase_item_name,
                        pib.name AS purchase_item_brand_name,
                        pii.name AS purchase_inventory_item_name,
                        pib.name AS purchase_inventory_item_brand_name,
                        pi.purchased AS purchase_item_purchased,
                        pbm.updated_at,
                        COUNT(DISTINCT CASE
                            WHEN COALESCE(f.s3_key, f.original_s3_key, f.processed_s3_key, f.ai_enhanced_s3_key) IS NOT NULL
                            THEN f.face_name
                        END) AS active_face_count,
                        COUNT(DISTINCT CASE WHEN f.original_s3_key IS NOT NULL THEN f.face_name END) AS original_texture_count,
                        COUNT(DISTINCT CASE WHEN f.processed_s3_key IS NOT NULL THEN f.face_name END) AS processed_texture_count,
                        COUNT(DISTINCT CASE WHEN f.ai_enhanced_s3_key IS NOT NULL THEN f.face_name END) AS ai_enhanced_texture_count,
                        COALESCE(STRING_AGG(DISTINCT f.texture_status, ', ' ORDER BY f.texture_status) FILTER (WHERE f.texture_status IS NOT NULL), '') AS texture_statuses,
                        COALESCE(STRING_AGG(DISTINCT f.ai_enhancement_status, ', ' ORDER BY f.ai_enhancement_status) FILTER (WHERE f.ai_enhancement_status IS NOT NULL), '') AS ai_statuses,
                        COALESCE(STRING_AGG(DISTINCT f.active_texture_source, ', ' ORDER BY f.active_texture_source) FILTER (WHERE f.active_texture_source IS NOT NULL), '') AS active_texture_sources
                    FROM product_box_models pbm
                    LEFT JOIN inventory_items ii
                        ON ii.id = pbm.inventory_item_id
                       AND ii.organization_id = pbm.organization_id
                       AND ii.deleted_at IS NULL
                    LEFT JOIN brands ib
                        ON ib.id = ii.brand_id
                       AND ib.organization_id = ii.organization_id
                       AND ib.deleted_at IS NULL
                    LEFT JOIN purchase_items pi
                        ON pi.id = pbm.purchase_item_id
                       AND pi.organization_id = pbm.organization_id
                    LEFT JOIN inventory_items pii
                        ON pii.id = pi.inventory_item_id
                       AND pii.organization_id = pi.organization_id
                       AND pii.deleted_at IS NULL
                    LEFT JOIN brands pib
                        ON pib.id = COALESCE(pii.brand_id, ii.brand_id)
                       AND pib.organization_id = pbm.organization_id
                       AND pib.deleted_at IS NULL
                    LEFT JOIN product_box_model_faces f
                        ON f.product_box_model_id = pbm.id
                       AND f.organization_id = pbm.organization_id
                    WHERE pbm.organization_id = :organizationId
                      AND pbm.deleted_at IS NULL
                    GROUP BY
                        pbm.id,
                        pbm.name,
                        pbm.description,
                        pbm.width,
                        pbm.height,
                        pbm.depth,
                        pbm.unit,
                        pbm.inventory_item_id,
                        ii.name,
                        ib.name,
                        ii.item_type,
                        pbm.purchase_item_id,
                        pi.item_name_snapshot,
                        pib.name,
                        pii.name,
                        pi.purchased,
                        pbm.updated_at
                )
                """;
    }

    private String missingFacesSql() {
        return """
                WITH expected_faces(face_name, display_order) AS (
                    VALUES
                        ('front', 1),
                        ('back', 2),
                        ('left', 3),
                        ('right', 4),
                        ('top', 5),
                        ('bottom', 6)
                ),
                model_stats AS (
                    SELECT
                        pbm.id AS model_id,
                        pbm.name AS model_name,
                        COALESCE(pbm.description, '') AS description,
                        pbm.width,
                        pbm.height,
                        pbm.depth,
                        pbm.unit,
                        pbm.inventory_item_id,
                        ii.name AS inventory_item_name,
                        ib.name AS inventory_item_brand_name,
                        ii.item_type AS inventory_item_type,
                        pbm.purchase_item_id,
                        pi.item_name_snapshot AS purchase_item_name,
                        pib.name AS purchase_item_brand_name,
                        pii.name AS purchase_inventory_item_name,
                        pib.name AS purchase_inventory_item_brand_name,
                        pi.purchased AS purchase_item_purchased,
                        pbm.updated_at,
                        COUNT(DISTINCT CASE
                            WHEN COALESCE(f.s3_key, f.original_s3_key, f.processed_s3_key, f.ai_enhanced_s3_key) IS NOT NULL
                            THEN f.face_name
                        END) AS active_face_count,
                        COUNT(DISTINCT CASE WHEN f.original_s3_key IS NOT NULL THEN f.face_name END) AS original_texture_count,
                        COUNT(DISTINCT CASE WHEN f.processed_s3_key IS NOT NULL THEN f.face_name END) AS processed_texture_count,
                        COUNT(DISTINCT CASE WHEN f.ai_enhanced_s3_key IS NOT NULL THEN f.face_name END) AS ai_enhanced_texture_count,
                        COALESCE(STRING_AGG(DISTINCT f.texture_status, ', ' ORDER BY f.texture_status) FILTER (WHERE f.texture_status IS NOT NULL), '') AS texture_statuses,
                        COALESCE(STRING_AGG(DISTINCT f.ai_enhancement_status, ', ' ORDER BY f.ai_enhancement_status) FILTER (WHERE f.ai_enhancement_status IS NOT NULL), '') AS ai_statuses,
                        COALESCE(STRING_AGG(DISTINCT f.active_texture_source, ', ' ORDER BY f.active_texture_source) FILTER (WHERE f.active_texture_source IS NOT NULL), '') AS active_texture_sources
                    FROM product_box_models pbm
                    LEFT JOIN inventory_items ii
                        ON ii.id = pbm.inventory_item_id
                       AND ii.organization_id = pbm.organization_id
                       AND ii.deleted_at IS NULL
                    LEFT JOIN brands ib
                        ON ib.id = ii.brand_id
                       AND ib.organization_id = ii.organization_id
                       AND ib.deleted_at IS NULL
                    LEFT JOIN purchase_items pi
                        ON pi.id = pbm.purchase_item_id
                       AND pi.organization_id = pbm.organization_id
                    LEFT JOIN inventory_items pii
                        ON pii.id = pi.inventory_item_id
                       AND pii.organization_id = pi.organization_id
                       AND pii.deleted_at IS NULL
                    LEFT JOIN brands pib
                        ON pib.id = COALESCE(pii.brand_id, ii.brand_id)
                       AND pib.organization_id = pbm.organization_id
                       AND pib.deleted_at IS NULL
                    LEFT JOIN product_box_model_faces f
                        ON f.product_box_model_id = pbm.id
                       AND f.organization_id = pbm.organization_id
                    WHERE pbm.organization_id = :organizationId
                      AND pbm.deleted_at IS NULL
                    GROUP BY
                        pbm.id,
                        pbm.name,
                        pbm.description,
                        pbm.width,
                        pbm.height,
                        pbm.depth,
                        pbm.unit,
                        pbm.inventory_item_id,
                        ii.name,
                        ib.name,
                        ii.item_type,
                        pbm.purchase_item_id,
                        pi.item_name_snapshot,
                        pib.name,
                        pii.name,
                        pi.purchased,
                        pbm.updated_at
                ),
                missing_faces AS (
                    SELECT
                        ms.model_id,
                        STRING_AGG(ef.face_name, ', ' ORDER BY ef.display_order) AS missing_faces
                    FROM model_stats ms
                    CROSS JOIN expected_faces ef
                    LEFT JOIN product_box_model_faces f
                        ON f.product_box_model_id = ms.model_id
                       AND f.organization_id = :organizationId
                       AND f.face_name = ef.face_name
                    WHERE COALESCE(f.s3_key, f.original_s3_key, f.processed_s3_key, f.ai_enhanced_s3_key) IS NULL
                    GROUP BY ms.model_id
                )
                """;
    }

    private String[] modelAliases() {
        return new String[]{
                "modelId",
                "modelName",
                "description",
                "width",
                "height",
                "depth",
                "unit",
                "inventoryItemId",
                "inventoryItemName",
                "inventoryItemBrandName",
                "inventoryItemType",
                "purchaseItemId",
                "purchaseItemName",
                "purchaseItemBrandName",
                "purchaseInventoryItemName",
                "purchaseInventoryItemBrandName",
                "purchaseItemPurchased",
                "activeFaceCount",
                "missingFaceCount",
                "originalTextureCount",
                "processedTextureCount",
                "aiEnhancedTextureCount",
                "textureStatuses",
                "aiStatuses",
                "activeTextureSources"
        };
    }

    private String extractProductBoxSearchText(String userQuestion) {
        if (userQuestion == null || userQuestion.isBlank()) {
            return null;
        }

        String search = normalizeForSearch(userQuestion);
        search = search.replaceAll(
                "\\b(product|box|productbox|productboxmodel|productboxmodels|caja|cajas|modelo|modelos|3d|textura|texturas|texture|textures|cara|caras|face|faces|inventario|inventory|item|items|producto|productos|compra|compras|purchase|purchases)\\b",
                " "
        );
        search = search.replaceAll(
                "\\b(que|cual|cuales|tiene|tienen|con|sin|los|las|el|la|un|una|unos|unas|de|del|al|a|y|o|en|por|para|muestra|mostrar|dame|lista|listar|ver|estado|status|resumen|summary|faltan|faltante|faltantes|incompleto|incompletos|incompleta|incompletas|esta|estan|asociado|asociados|asociada|asociadas|relacionado|relacionados|relacionada|relacionadas|ligado|ligados|ligada|ligadas|vinculado|vinculados|vinculada|vinculadas)\\b",
                " "
        );
        search = search.replaceAll("\\s+", " ").trim();
        if (search.length() > 120) {
            search = search.substring(0, 120).trim();
        }
        return nullableSearch(search);
    }

    private String normalizeForSearch(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
