package com.tamias.ai.dto;

import java.util.List;

public record AiSearchResponse(
        String question,
        Integer sourceCount,
        List<AiSourceResponse> sources
) {
}
