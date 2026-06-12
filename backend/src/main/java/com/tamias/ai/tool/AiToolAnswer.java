package com.tamias.ai.tool;

import com.tamias.ai.dto.AiToolEvidenceResponse;
import java.util.List;
import java.util.Map;

public record AiToolAnswer(
    String answer,
    boolean grounded,
    List<AiToolEvidenceResponse> evidence
) {
    public AiToolAnswer {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static AiToolAnswer of(
        String answer,
        String toolName,
        String label,
        String summary,
        List<Map<String, Object>> items
    ) {
        return new AiToolAnswer(
            answer,
            true,
            List.of(new AiToolEvidenceResponse(toolName, label, summary, items))
        );
    }
}
