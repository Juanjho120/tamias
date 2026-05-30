package com.tamias.maintenance.dto;

import com.tamias.maintenance.enums.MaintenanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceRecordRequest(
        @NotNull(message = "Property is required")
        UUID propertyId,

        UUID maintenanceCategoryId,

        UUID maintenanceTypeId,

        UUID maintenancePersonId,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        String description,

        OffsetDateTime scheduledAt,

        OffsetDateTime performedAt,

        @DecimalMin(value = "0.00", message = "Cost must be greater than or equal to 0")
        BigDecimal cost,

        @NotNull(message = "Status is required")
        MaintenanceStatus status
) {
}
