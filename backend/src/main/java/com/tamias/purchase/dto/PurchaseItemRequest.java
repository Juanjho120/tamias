package com.tamias.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemRequest(
        UUID inventoryItemId,
        UUID materialId,
        @Size(max = 150) String itemNameSnapshot,
        @DecimalMin(value = "0.01") BigDecimal quantity,
        @Size(max = 50) String unit,
        @DecimalMin(value = "0.00") BigDecimal estimatedPrice,
        Boolean purchased,
        String notes
) {
    public UUID requestedInventoryItemId() {
        return inventoryItemId != null ? inventoryItemId : materialId;
    }
}
