package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findByChatSession_IdAndOrganization_IdOrderByCreatedAtAsc(
            UUID chatSessionId,
            UUID organizationId
    );

    long countByChatSession_IdAndOrganization_Id(UUID chatSessionId, UUID organizationId);
}
