package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssistantProfileReadOnlyToolService extends AiReadOnlyToolSupport {

    public AssistantProfileReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer capabilities() {
        return super.capabilities();
    }

    public AiToolAnswer currentUserProfile(String userQuestion) {
        return super.currentUserProfile(userQuestion);
    }

    public AiToolAnswer currentOrganizationSummary() {
        return super.currentOrganizationSummary();
    }

}
