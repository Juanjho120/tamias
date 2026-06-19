package com.tamias.productbox.dto;

import com.tamias.productbox.enums.ProductBoxUnit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductBoxModelResponse(
    UUID id,
    String name,
    String description,
    UUID inventoryItemId,
    String inventoryItemName,
    UUID inventoryItemBrandId,
    String inventoryItemBrandName,
    UUID purchaseItemId,
    String purchaseItemNameSnapshot,
    UUID purchaseListId,
    BigDecimal width,
    BigDecimal height,
    BigDecimal depth,
    ProductBoxUnit unit,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
