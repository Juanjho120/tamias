package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.AiReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AssistantCoreToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public AssistantCoreToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        String question = context.question();
        String normalized = context.normalizedQuestion();

        if (isUnsupportedWriteAction(normalized)) {
            return Optional.of(readOnlyGuard());
        }
        if (isCapabilitiesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.capabilities());
        }
        if (isCurrentUserProfileQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentUserProfile(question));
        }
        return Optional.empty();
    }



}
