package com.tamias.task.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskItemResponse(
        UUID id,
        UUID taskTemplateId,
        String taskTemplateName,
        String taskName,
        String responsiblePerson,
        Boolean completed,
        OffsetDateTime completionDate,
        Integer sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
