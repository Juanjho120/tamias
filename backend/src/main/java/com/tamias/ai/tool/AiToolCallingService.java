package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.handler.AiToolHandler;
import com.tamias.ai.tool.support.AiToolFallbackPolicy;
import com.tamias.ai.tool.support.AiToolTextNormalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiToolCallingService {

    private final List<AiToolHandler> handlers;

    public AiToolCallingService(List<AiToolHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public Optional<AiToolAnswer> tryHandle(AiChatRequest request) {
        AiToolResult result = tryHandleResult(request);
        if (result.status() == AiToolResultStatus.NOT_APPLICABLE) {
            return Optional.empty();
        }
        return result.answerOptional();
    }

    public AiToolResult tryHandleResult(AiChatRequest request) {
        String question = request.question();
        String normalized = AiToolTextNormalizer.normalizeForRouting(question);
        AiToolRequestContext context = new AiToolRequestContext(request, question, normalized);

        for (AiToolHandler handler : handlers) {
            Optional<AiToolAnswer> answer = handler.tryHandle(context);
            if (answer.isPresent()) {
                AiToolResult classified = AiToolFallbackPolicy.classify(answer.get(), normalized);
                return classified.withTrace(
                        handler.getClass().getSimpleName(),
                        toolNames(answer.get()),
                        traceParams(request, normalized, answer.get(), classified)
                );
            }
        }

        return AiToolResult.notApplicable().withTrace(null, List.of(), traceParams(request, normalized, null, null));
    }

    private List<String> toolNames(AiToolAnswer answer) {
        if (answer == null || answer.evidence() == null) {
            return List.of();
        }
        return answer.evidence().stream()
                .map(AiToolEvidenceResponse::toolName)
                .filter(toolName -> toolName != null && !toolName.isBlank())
                .distinct()
                .toList();
    }

    private Map<String, Object> traceParams(
            AiChatRequest request,
            String normalizedQuestion,
            AiToolAnswer answer,
            AiToolResult result
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("question", request.question());
        params.put("normalizedQuestion", normalizedQuestion);
        if (request.chatSessionId() != null) {
            params.put("chatSessionId", request.chatSessionId().toString());
        }
        if (request.propertyId() != null) {
            params.put("propertyId", request.propertyId().toString());
        }
        if (request.topK() != null) {
            params.put("topK", request.topK());
        }
        if (request.similarityThreshold() != null) {
            params.put("similarityThreshold", request.similarityThreshold());
        }
        if (result != null) {
            params.put("toolResultStatus", result.status().name());
            params.put("allowRagFallback", result.allowRagFallback());
        }
        if (answer != null && answer.evidence() != null) {
            params.put("evidenceCount", answer.evidence().size());
            int evidenceItemCount = answer.evidence().stream()
                    .map(AiToolEvidenceResponse::items)
                    .filter(items -> items != null)
                    .mapToInt(List::size)
                    .sum();
            params.put("evidenceItemCount", evidenceItemCount);
        }
        return params;
    }
}
