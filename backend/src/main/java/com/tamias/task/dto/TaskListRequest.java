package com.tamias.task.dto;

import com.tamias.task.enums.TaskListStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskListRequest(
        @NotNull(message = "Property is required")
        UUID propertyId,

        UUID reservationId,

        UUID maintenanceRecordId,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        LocalDate creationDate,

        LocalDate dueDate,

        @NotNull(message = "Status is required")
        TaskListStatus status,

        @Valid
        List<TaskItemRequest> items
) {
}
