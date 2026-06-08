package com.tamias.ai.dto;

import java.util.List;

public record AiChatResponse(
        String question,
        String answer,
        Boolean grounded,
        Integer sourceCount,
        List<AiSourceResponse> sources
) {
}
