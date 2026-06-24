package com.tamias.ai.tool.support;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class AiReadOnlyToolSupport extends AiToolAccessSupport {

    protected AiReadOnlyToolSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }
}
