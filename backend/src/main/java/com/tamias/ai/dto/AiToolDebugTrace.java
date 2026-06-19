package com.tamias.ai.dto;

import com.tamias.ai.enums.AiAnswerSource;
import java.util.List;
import java.util.Map;

public record AiToolDebugTrace(
        String handler,
        String toolName,
        List<String> toolNames,
        Map<String, Object> params,
        Boolean ragUsed,
        AiAnswerSource answerSource,
        String routeReason,
        String fallbackReason,
        String errorMessage
) {
    public AiToolDebugTrace {
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
        params = params == null ? Map.of() : Map.copyOf(params);
        ragUsed = ragUsed != null && ragUsed;
        answerSource = answerSource == null ? AiAnswerSource.NO_MATCH : answerSource;
    }
}
