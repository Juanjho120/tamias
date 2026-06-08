package com.tamias.scheduledmaintenance.dto;

import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceFrequency;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledMaintenanceResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID maintenanceCategoryId,
        String maintenanceCategoryName,
        UUID maintenanceTypeId,
        String maintenanceTypeName,
        UUID maintenancePersonId,
        String maintenancePersonName,
        String title,
        String description,
        ScheduledMaintenanceFrequency frequency,
        Integer intervalValue,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextDueDate,
        OffsetDateTime lastGeneratedAt,
        BigDecimal estimatedCost,
        ScheduledMaintenanceStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
