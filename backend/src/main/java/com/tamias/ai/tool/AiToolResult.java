package com.tamias.ai.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record AiToolResult(
        AiToolResultStatus status,
        AiToolAnswer answer,
        boolean allowRagFallback,
        String handler,
        List<String> toolNames,
        Map<String, Object> params
) {

    public AiToolResult {
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public static AiToolResult hit(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.HIT, answer, false, null, List.of(), Map.of());
    }

    public static AiToolResult empty(AiToolAnswer answer, boolean allowRagFallback) {
        return new AiToolResult(AiToolResultStatus.EMPTY, answer, allowRagFallback, null, List.of(), Map.of());
    }

    public static AiToolResult denied(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.DENIED, answer, false, null, List.of(), Map.of());
    }

    public static AiToolResult guardrail(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.GUARDRAIL, answer, false, null, List.of(), Map.of());
    }

    public static AiToolResult error(AiToolAnswer answer, boolean allowRagFallback) {
        return new AiToolResult(AiToolResultStatus.ERROR, answer, allowRagFallback, null, List.of(), Map.of());
    }

    public static AiToolResult notApplicable() {
        return new AiToolResult(AiToolResultStatus.NOT_APPLICABLE, null, true, null, List.of(), Map.of());
    }

    public AiToolResult withTrace(String handler, List<String> toolNames, Map<String, Object> params) {
        return new AiToolResult(status, answer, allowRagFallback, handler, toolNames, params);
    }

    public Optional<AiToolAnswer> answerOptional() {
        return Optional.ofNullable(answer);
    }

    public boolean hasAnswer() {
        return answer != null;
    }

    public boolean shouldAttemptRagFallback() {
        return allowRagFallback && status.allowsRagFallback();
    }

    public boolean shouldRespondImmediately() {
        return hasAnswer() && !shouldAttemptRagFallback();
    }
}
