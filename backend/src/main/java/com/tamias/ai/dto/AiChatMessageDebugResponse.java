package com.tamias.ai.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiChatMessageDebugResponse(
        UUID id,
        UUID aiChatMessageId,
        String handler,
        String toolName,
        List<String> toolNames,
        Map<String, Object> params,
        Boolean ragUsed,
        String answerSource,
        String routeReason,
        String fallbackReason,
        String errorMessage,
        OffsetDateTime createdAt
) {
    public AiChatMessageDebugResponse {
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
