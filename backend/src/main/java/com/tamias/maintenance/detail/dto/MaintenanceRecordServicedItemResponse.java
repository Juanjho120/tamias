package com.tamias.maintenance.detail.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceRecordServicedItemResponse(
        UUID id,
        UUID maintenanceRecordId,
        UUID inventoryItemId,
        String inventoryItemName,
        UUID inventoryItemBrandId,
        String inventoryItemBrandName,
        String itemNameSnapshot,
        BigDecimal quantity,
        String unit,
        String notes
) {
}
