package com.tamias.purchase.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseItemResponse(
        UUID id,
        UUID materialId,
        String materialName,
        UUID brandId,
        String brandName,
        String itemNameSnapshot,
        BigDecimal quantity,
        String unit,
        BigDecimal estimatedPrice,
        Boolean purchased,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
