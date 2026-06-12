package com.tamias.ai.dto;

import java.util.List;
import java.util.Map;

public record AiToolEvidenceResponse(
    String toolName,
    String label,
    String summary,
    List<Map<String, Object>> items
) {
    public AiToolEvidenceResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
