package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatMessageDebug;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatMessageDebugRepository extends JpaRepository<AiChatMessageDebug, UUID> {

    Optional<AiChatMessageDebug> findByAiChatMessage_Id(UUID aiChatMessageId);

    @Modifying
    @Query("""
            DELETE FROM AiChatMessageDebug debug
            WHERE debug.aiChatMessage.chatSession.id = :sessionId
            """)
    void deleteByChatSessionId(@Param("sessionId") UUID sessionId);
}
