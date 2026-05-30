package com.tamias.catalog.tasktemplate.repository;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, UUID> {

    Optional<TaskTemplate> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndTitleIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String title);

    Page<TaskTemplate> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<TaskTemplate> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
