package com.tamias.ai.dto;

import java.util.List;

public record AiChatResponse(
        String question,
        String answer,
        List<AiSourceResponse> sources
) {
}
