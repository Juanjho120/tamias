package com.tamias.task.dto;

import com.tamias.task.enums.TaskListStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskListSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID reservationId,
        UUID maintenanceRecordId,
        String title,
        LocalDate creationDate,
        LocalDate dueDate,
        TaskListStatus status,
        long totalItems,
        long completedItems,
        OffsetDateTime createdAt
) {
}
