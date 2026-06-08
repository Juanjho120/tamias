package com.tamias.scheduledmaintenance.dto;

import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceFrequency;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledMaintenanceSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID maintenanceCategoryId,
        String maintenanceCategoryName,
        UUID maintenanceTypeId,
        String maintenanceTypeName,
        String title,
        ScheduledMaintenanceFrequency frequency,
        Integer intervalValue,
        LocalDate nextDueDate,
        BigDecimal estimatedCost,
        ScheduledMaintenanceStatus status,
        OffsetDateTime createdAt
) {
}
