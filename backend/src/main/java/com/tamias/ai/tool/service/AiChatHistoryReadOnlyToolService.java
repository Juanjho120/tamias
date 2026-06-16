package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.AiChatHistoryToolRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiChatHistoryReadOnlyToolService {

    private final AiChatHistoryToolRepository repository;

    public AiChatHistoryReadOnlyToolService(AiChatHistoryToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer aiChatRecentSessions(UUID excludedSessionId) {
        return repository.aiChatRecentSessions(excludedSessionId);
    }

    public AiToolAnswer aiChatSearchHistory(String userQuestion, UUID excludedSessionId) {
        return repository.aiChatSearchHistory(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatRecentMessages(UUID excludedSessionId) {
        return repository.aiChatRecentMessages(excludedSessionId);
    }

    public AiToolAnswer aiChatRecentUserQuestions(UUID excludedSessionId) {
        return repository.aiChatRecentUserQuestions(excludedSessionId);
    }

    public AiToolAnswer aiChatSessionsByProperty(String userQuestion, UUID excludedSessionId) {
        return repository.aiChatSessionsByProperty(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatCurrentSessionSummary(UUID chatSessionId) {
        return repository.aiChatCurrentSessionSummary(chatSessionId);
    }

    public AiToolAnswer aiChatUsageSummary() {
        return repository.aiChatUsageSummary();
    }
}
