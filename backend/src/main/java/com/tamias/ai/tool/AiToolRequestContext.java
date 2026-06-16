package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;

public record AiToolRequestContext(
        AiChatRequest request,
        String question,
        String normalizedQuestion
) {
}
