package com.tamias.maintenance.repository;

import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.maintenance.enums.MaintenanceStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Optional<MaintenanceRecord> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<MaintenanceRecord> findByOrganization_IdAndDeletedAtIsNull(
            UUID organizationId,
            Pageable pageable
    );

    Page<MaintenanceRecord> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            MaintenanceStatus status,
            Pageable pageable
    );

    Page<MaintenanceRecord> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<MaintenanceRecord> findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            MaintenanceStatus status,
            Pageable pageable
    );
}
