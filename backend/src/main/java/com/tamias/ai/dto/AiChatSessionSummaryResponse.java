package com.tamias.ai.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatSessionSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String title,
        UUID createdBy,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long messageCount
) {
}
