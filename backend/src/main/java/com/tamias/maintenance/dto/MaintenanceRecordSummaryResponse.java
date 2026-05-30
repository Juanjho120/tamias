package com.tamias.maintenance.dto;

import com.tamias.maintenance.enums.MaintenanceStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceRecordSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID maintenanceTypeId,
        String maintenanceTypeName,
        String title,
        OffsetDateTime scheduledAt,
        OffsetDateTime performedAt,
        BigDecimal cost,
        MaintenanceStatus status,
        OffsetDateTime createdAt
) {
}
