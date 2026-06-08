package com.tamias.task.dto;

import com.tamias.task.enums.TaskListStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TaskListResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID reservationId,
        UUID maintenanceRecordId,
        String title,
        LocalDate creationDate,
        LocalDate dueDate,
        TaskListStatus status,
        List<TaskItemResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
