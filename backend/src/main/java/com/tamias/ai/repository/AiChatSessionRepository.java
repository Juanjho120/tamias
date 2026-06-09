package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {

    Optional<AiChatSession> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Page<AiChatSession> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<AiChatSession> findByOrganization_IdAndProperty_Id(UUID organizationId, UUID propertyId, Pageable pageable);
}
