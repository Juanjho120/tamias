package com.tamias.ai.planning;

public enum AiPlanDecisionType {
    TOOL_FIRST,
    RAG_FIRST,
    TOOL_ONLY,
    RAG_ONLY,
    TOOL_AND_RAG,
    CLARIFY,
    DENY_WRITE
}
