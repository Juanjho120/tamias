package com.tamias.scheduledmaintenance.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduledMaintenanceRescheduleRequest(
        @NotNull(message = "Next due date is required")
        LocalDate nextDueDate,

        String reason
) {
}
