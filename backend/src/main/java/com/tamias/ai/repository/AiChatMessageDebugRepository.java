package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatMessageDebug;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageDebugRepository extends JpaRepository<AiChatMessageDebug, UUID> {

    Optional<AiChatMessageDebug> findByAiChatMessage_Id(UUID aiChatMessageId);
}
