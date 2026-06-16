package com.tamias.ai.tool;

public enum AiToolResultStatus {
    HIT,
    EMPTY,
    DENIED,
    GUARDRAIL,
    NOT_APPLICABLE,
    ERROR;

    public boolean allowsRagFallback() {
        return this == EMPTY || this == ERROR || this == NOT_APPLICABLE;
    }

    public boolean isTerminalWithoutFallback() {
        return this == HIT || this == DENIED || this == GUARDRAIL || (this == ERROR);
    }
}
