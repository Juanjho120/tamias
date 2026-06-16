package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AiChatHistoryReadOnlyToolService extends AiReadOnlyToolSupport {

    public AiChatHistoryReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer aiChatRecentSessions(UUID excludedSessionId) {
        return super.aiChatRecentSessions(excludedSessionId);
    }

    public AiToolAnswer aiChatSearchHistory(String userQuestion, UUID excludedSessionId) {
        return super.aiChatSearchHistory(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatRecentMessages(UUID excludedSessionId) {
        return super.aiChatRecentMessages(excludedSessionId);
    }

    public AiToolAnswer aiChatRecentUserQuestions(UUID excludedSessionId) {
        return super.aiChatRecentUserQuestions(excludedSessionId);
    }

    public AiToolAnswer aiChatSessionsByProperty(String userQuestion, UUID excludedSessionId) {
        return super.aiChatSessionsByProperty(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatCurrentSessionSummary(UUID chatSessionId) {
        return super.aiChatCurrentSessionSummary(chatSessionId);
    }

    public AiToolAnswer aiChatUsageSummary() {
        return super.aiChatUsageSummary();
    }

}
