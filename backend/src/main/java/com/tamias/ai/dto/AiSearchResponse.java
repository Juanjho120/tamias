package com.tamias.ai.dto;

import java.util.List;

public record AiSearchResponse(
        String question,
        List<AiSourceResponse> sources
) {
}
