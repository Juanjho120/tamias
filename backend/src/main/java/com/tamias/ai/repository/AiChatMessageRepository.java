package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findByChatSession_IdAndOrganization_IdOrderByCreatedAtAsc(
            UUID chatSessionId,
            UUID organizationId
    );

    long countByChatSession_IdAndOrganization_Id(UUID chatSessionId, UUID organizationId);

    @Modifying
    @Query("""
            DELETE FROM AiChatMessage message
            WHERE message.chatSession.id = :sessionId
              AND message.organization.id = :organizationId
            """)
    void deleteByChatSessionIdAndOrganizationId(
            @Param("sessionId") UUID sessionId,
            @Param("organizationId") UUID organizationId
    );
}
