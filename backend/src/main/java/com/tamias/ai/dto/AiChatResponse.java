package com.tamias.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(
        UUID chatSessionId,
        UUID userMessageId,
        UUID assistantMessageId,
        String question,
        String answer,
        Boolean grounded,
        Integer sourceCount,
        List<AiSourceResponse> sources
) {
}
