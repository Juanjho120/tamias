package com.tamias.ai.tool.support;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;

public abstract class AiBaseReadOnlyToolSupport {

    protected static final int DEFAULT_LIMIT = 10;

    protected final EntityManager entityManager;
    protected final CurrentUserService currentUserService;

    protected AiBaseReadOnlyToolSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
    }
}
