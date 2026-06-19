package com.tamias.ai.dto;

import com.tamias.ai.enums.AiChatMessageRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AiChatMessageResponse(
        UUID id,
        UUID chatSessionId,
        AiChatMessageRole role,
        String content,
        OffsetDateTime createdAt,
        AiChatMessageDebugResponse debug
) {
    public AiChatMessageResponse(
            UUID id,
            UUID chatSessionId,
            AiChatMessageRole role,
            String content,
            OffsetDateTime createdAt
    ) {
        this(id, chatSessionId, role, content, createdAt, null);
    }
}
