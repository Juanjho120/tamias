package com.tamias.catalog.maintenancetype.repository;

import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenancetype.entity.MaintenanceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTypeRepository extends JpaRepository<MaintenanceType, UUID> {

    Optional<MaintenanceType> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String name);

    Page<MaintenanceType> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<MaintenanceType> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
