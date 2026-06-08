package com.tamias.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TaskItemUpdateRequest(
        UUID taskTemplateId,

        @NotBlank(message = "Task name is required")
        @Size(max = 150, message = "Task name must not exceed 150 characters")
        String taskName,

        @Size(max = 150, message = "Responsible person must not exceed 150 characters")
        String responsiblePerson,

        Boolean completed,

        Integer sortOrder
) {
}
