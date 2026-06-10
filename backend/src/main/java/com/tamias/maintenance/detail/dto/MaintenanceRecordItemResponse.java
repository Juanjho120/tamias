package com.tamias.maintenance.detail.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceRecordItemResponse(
        UUID id,
        UUID maintenanceRecordId,
        UUID inventoryItemId,
        String inventoryItemName,
        UUID materialId,
        String materialName,
        String itemNameSnapshot,
        String materialNameSnapshot,
        BigDecimal quantity,
        String unit,
        String notes
) {
}
