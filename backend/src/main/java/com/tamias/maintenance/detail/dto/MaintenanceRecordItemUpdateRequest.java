package com.tamias.maintenance.detail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceRecordItemUpdateRequest(
        UUID inventoryItemId,

        UUID materialId,

        @Size(max = 150, message = "Item name must not exceed 150 characters")
        String itemNameSnapshot,

        @Size(max = 150, message = "Material name must not exceed 150 characters")
        String materialNameSnapshot,

        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        String notes
) {
    public UUID requestedInventoryItemId() {
        return inventoryItemId != null ? inventoryItemId : materialId;
    }

    public String requestedItemNameSnapshot() {
        return itemNameSnapshot != null && !itemNameSnapshot.isBlank()
                ? itemNameSnapshot
                : materialNameSnapshot;
    }
}
