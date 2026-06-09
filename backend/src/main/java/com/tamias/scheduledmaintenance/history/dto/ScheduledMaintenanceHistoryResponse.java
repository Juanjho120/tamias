package com.tamias.scheduledmaintenance.history.dto;

import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduledMaintenanceHistoryResponse(
        UUID id,
        UUID scheduledMaintenanceId,
        ScheduledMaintenanceStatus previousStatus,
        ScheduledMaintenanceStatus newStatus,
        LocalDate previousPlannedDate,
        LocalDate newPlannedDate,
        LocalTime previousPlannedTime,
        LocalTime newPlannedTime,
        String reason,
        UUID changedBy,
        String changedByName,
        OffsetDateTime changedAt
) {
}
