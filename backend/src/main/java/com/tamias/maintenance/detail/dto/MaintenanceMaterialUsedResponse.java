package com.tamias.maintenance.detail.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceMaterialUsedResponse(
        UUID id,
        UUID maintenanceRecordId,
        UUID materialId,
        String materialName,
        String materialNameSnapshot,
        BigDecimal quantity,
        String unit,
        String notes
) {
}
