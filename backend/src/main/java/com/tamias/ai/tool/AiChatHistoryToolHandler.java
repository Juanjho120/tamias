package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class AiChatHistoryToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public AiChatHistoryToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleAiChatHistoryQuestion(context.request(), context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleAiChatHistoryQuestion(AiChatRequest request, String question, String normalized) {
        if (!isAiChatHistoryQuestion(normalized)) {
            return Optional.empty();
        }
        if (isAiChatUsageSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatUsageSummary());
        }
        if (isAiChatCurrentSessionQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatCurrentSessionSummary(request.chatSessionId()));
        }
        if (isAiChatByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatSessionsByProperty(question, request.chatSessionId()));
        }
        if (isAiChatRecentMessagesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatRecentUserQuestions(request.chatSessionId()));
        }
        if (isAiChatSearchHistoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatSearchHistory(question, request.chatSessionId()));
        }
        return Optional.of(readOnlyToolService.aiChatRecentSessions(request.chatSessionId()));
    }
}
