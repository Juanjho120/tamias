package com.tamias.catalog.maintenanceperson.repository;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePersonRepository extends JpaRepository<MaintenancePerson, UUID> {

    Optional<MaintenancePerson> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndFullNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String fullName);

    Page<MaintenancePerson> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<MaintenancePerson> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
