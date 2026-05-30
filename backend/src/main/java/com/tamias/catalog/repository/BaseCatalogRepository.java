package com.tamias.catalog.repository;

import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.enums.CatalogStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;

@NoRepositoryBean
public interface BaseCatalogRepository<T extends BaseCatalogEntity> extends JpaRepository<T, UUID> {

    Optional<T> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String name);

    Page<T> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<T> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
