package com.tamias.ai.planning;

public record AiPlanResponse(
        String decision,
        String reason,
        Double confidence
) {
}
