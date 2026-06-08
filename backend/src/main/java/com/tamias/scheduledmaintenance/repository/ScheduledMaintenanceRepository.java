package com.tamias.scheduledmaintenance.repository;

import com.tamias.scheduledmaintenance.entity.ScheduledMaintenance;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledMaintenanceRepository extends JpaRepository<ScheduledMaintenance, UUID> {

    Optional<ScheduledMaintenance> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<ScheduledMaintenance> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<ScheduledMaintenance> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            ScheduledMaintenanceStatus status,
            Pageable pageable
    );

    Page<ScheduledMaintenance> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<ScheduledMaintenance> findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            ScheduledMaintenanceStatus status,
            Pageable pageable
    );

    Page<ScheduledMaintenance> findByOrganization_IdAndNextDueDateLessThanEqualAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            LocalDate date,
            ScheduledMaintenanceStatus status,
            Pageable pageable
    );
}
