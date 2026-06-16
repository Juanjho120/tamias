package com.tamias.ai.planning;

public record AiExecutionPlan(
        AiPlanDecisionType decision,
        String reason,
        double confidence,
        boolean llmGenerated
) {

    public static AiExecutionPlan defaultPlan(String reason) {
        return new AiExecutionPlan(AiPlanDecisionType.TOOL_FIRST, reason, 0.0, false);
    }

    public static AiExecutionPlan ragOnly(String reason) {
        return new AiExecutionPlan(AiPlanDecisionType.RAG_ONLY, reason, 1.0, false);
    }

    public static AiExecutionPlan toolFirst(String reason) {
        return new AiExecutionPlan(AiPlanDecisionType.TOOL_FIRST, reason, 1.0, false);
    }

    public static AiExecutionPlan denyWrite(String reason) {
        return new AiExecutionPlan(AiPlanDecisionType.DENY_WRITE, reason, 1.0, false);
    }

    public AiPlanDecisionType safeDecision() {
        return decision == null ? AiPlanDecisionType.TOOL_FIRST : decision;
    }

    public boolean prefersRagFirst() {
        return safeDecision() == AiPlanDecisionType.RAG_FIRST
                || safeDecision() == AiPlanDecisionType.RAG_ONLY;
    }

    public boolean wantsTool() {
        return safeDecision() == AiPlanDecisionType.TOOL_FIRST
                || safeDecision() == AiPlanDecisionType.TOOL_ONLY
                || safeDecision() == AiPlanDecisionType.TOOL_AND_RAG
                || safeDecision() == AiPlanDecisionType.RAG_FIRST;
    }

    public boolean wantsRag() {
        return safeDecision() == AiPlanDecisionType.RAG_FIRST
                || safeDecision() == AiPlanDecisionType.RAG_ONLY
                || safeDecision() == AiPlanDecisionType.TOOL_AND_RAG
                || safeDecision() == AiPlanDecisionType.TOOL_FIRST;
    }

    public boolean wantsBoth() {
        return safeDecision() == AiPlanDecisionType.TOOL_AND_RAG;
    }

    public boolean toolOnly() {
        return safeDecision() == AiPlanDecisionType.TOOL_ONLY;
    }

    public boolean ragOnly() {
        return safeDecision() == AiPlanDecisionType.RAG_ONLY;
    }

    public boolean shouldAskClarification() {
        return safeDecision() == AiPlanDecisionType.CLARIFY;
    }

    public boolean shouldDenyWrite() {
        return safeDecision() == AiPlanDecisionType.DENY_WRITE;
    }
}
