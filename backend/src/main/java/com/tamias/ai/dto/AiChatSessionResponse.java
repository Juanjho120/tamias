package com.tamias.ai.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AiChatSessionResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        String title,
        UUID createdBy,
        String createdByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AiChatMessageResponse> messages
) {
}
