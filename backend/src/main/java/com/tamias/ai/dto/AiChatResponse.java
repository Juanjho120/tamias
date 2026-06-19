package com.tamias.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiChatResponse(
    UUID chatSessionId,
    UUID userMessageId,
    UUID assistantMessageId,
    String question,
    String answer,
    Boolean grounded,
    Integer sourceCount,
    List<AiSourceResponse> sources,
    List<AiToolEvidenceResponse> toolEvidence,
    AiChatMessageDebugResponse debug
) {
    public AiChatResponse(
        UUID chatSessionId,
        UUID userMessageId,
        UUID assistantMessageId,
        String question,
        String answer,
        Boolean grounded,
        Integer sourceCount,
        List<AiSourceResponse> sources
    ) {
        this(chatSessionId, userMessageId, assistantMessageId, question, answer, grounded, sourceCount, sources, List.of(), null);
    }

    public AiChatResponse(
        UUID chatSessionId,
        UUID userMessageId,
        UUID assistantMessageId,
        String question,
        String answer,
        Boolean grounded,
        Integer sourceCount,
        List<AiSourceResponse> sources,
        List<AiToolEvidenceResponse> toolEvidence
    ) {
        this(chatSessionId, userMessageId, assistantMessageId, question, answer, grounded, sourceCount, sources, toolEvidence, null);
    }

    public AiChatResponse {
        sources = sources == null ? List.of() : List.copyOf(sources);
        toolEvidence = toolEvidence == null ? List.of() : List.copyOf(toolEvidence);
    }
}
