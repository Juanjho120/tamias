package com.tamias.ai.tool;

import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.handler.AiToolHandler;
import com.tamias.ai.tool.support.AiToolTextNormalizer;
import com.tamias.ai.dto.AiChatRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiToolCallingService {

    private final List<AiToolHandler> handlers;

    public AiToolCallingService(List<AiToolHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    public Optional<AiToolAnswer> tryHandle(AiChatRequest request) {
        return tryHandleResult(request).answerOptional();
    }

    public AiToolResult tryHandleResult(AiChatRequest request) {
        String question = request.question();
        String normalized = AiToolTextNormalizer.normalizeForRouting(question);
        AiToolRequestContext context = new AiToolRequestContext(request, question, normalized);

        for (AiToolHandler handler : handlers) {
            Optional<AiToolAnswer> answer = handler.tryHandle(context);
            if (answer.isPresent()) {
                return AiToolResult.hit(answer.get());
            }
        }

        return AiToolResult.notApplicable();
    }
}
