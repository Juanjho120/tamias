package com.tamias.scheduledmaintenance.dto;

import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceFrequency;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduledMaintenanceRequest(
        @NotNull(message = "Property is required")
        UUID propertyId,

        UUID maintenanceCategoryId,

        UUID maintenanceTypeId,

        UUID maintenancePersonId,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        String description,

        @NotNull(message = "Frequency is required")
        ScheduledMaintenanceFrequency frequency,

        @NotNull(message = "Interval value is required")
        @Min(value = 1, message = "Interval value must be greater than or equal to 1")
        Integer intervalValue,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        LocalDate nextDueDate,

        @DecimalMin(value = "0.00", message = "Estimated cost must be greater than or equal to 0")
        BigDecimal estimatedCost,

        @NotNull(message = "Status is required")
        ScheduledMaintenanceStatus status
) {
}
