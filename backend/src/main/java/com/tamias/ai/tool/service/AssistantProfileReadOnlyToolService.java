package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.AssistantProfileToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssistantProfileReadOnlyToolService {

    private final AssistantProfileToolRepository repository;

    public AssistantProfileReadOnlyToolService(AssistantProfileToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer capabilities() {
        return repository.capabilities();
    }

    public AiToolAnswer currentUserProfile(String userQuestion) {
        return repository.currentUserProfile(userQuestion);
    }

    public AiToolAnswer currentOrganizationSummary() {
        return repository.currentOrganizationSummary();
    }
}
