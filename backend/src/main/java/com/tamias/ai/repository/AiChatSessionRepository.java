package com.tamias.ai.repository;

import com.tamias.ai.entity.AiChatSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, UUID> {

    Optional<AiChatSession> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Optional<AiChatSession> findByIdAndOrganization_IdAndCreatedBy_Id(UUID id, UUID organizationId, UUID createdById);

    Page<AiChatSession> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<AiChatSession> findByOrganization_IdAndCreatedBy_Id(UUID organizationId, UUID createdById, Pageable pageable);

    Page<AiChatSession> findByOrganization_IdAndProperty_Id(UUID organizationId, UUID propertyId, Pageable pageable);

    Page<AiChatSession> findByOrganization_IdAndCreatedBy_IdAndProperty_Id(
            UUID organizationId,
            UUID createdById,
            UUID propertyId,
            Pageable pageable
    );
}
