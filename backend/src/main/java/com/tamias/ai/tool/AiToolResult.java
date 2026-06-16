package com.tamias.ai.tool;

import java.util.Optional;

public record AiToolResult(
        AiToolResultStatus status,
        AiToolAnswer answer,
        boolean allowRagFallback
) {

    public static AiToolResult hit(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.HIT, answer, false);
    }

    public static AiToolResult empty(AiToolAnswer answer, boolean allowRagFallback) {
        return new AiToolResult(AiToolResultStatus.EMPTY, answer, allowRagFallback);
    }

    public static AiToolResult denied(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.DENIED, answer, false);
    }

    public static AiToolResult guardrail(AiToolAnswer answer) {
        return new AiToolResult(AiToolResultStatus.GUARDRAIL, answer, false);
    }

    public static AiToolResult error(AiToolAnswer answer, boolean allowRagFallback) {
        return new AiToolResult(AiToolResultStatus.ERROR, answer, allowRagFallback);
    }

    public static AiToolResult notApplicable() {
        return new AiToolResult(AiToolResultStatus.NOT_APPLICABLE, null, true);
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
