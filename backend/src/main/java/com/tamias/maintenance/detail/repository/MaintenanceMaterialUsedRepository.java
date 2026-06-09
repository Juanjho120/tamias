package com.tamias.maintenance.detail.repository;

import com.tamias.maintenance.detail.entity.MaintenanceMaterialUsed;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceMaterialUsedRepository extends JpaRepository<MaintenanceMaterialUsed, UUID> {

    List<MaintenanceMaterialUsed> findByMaintenanceRecord_IdAndOrganization_IdOrderByIdAsc(
            UUID maintenanceRecordId,
            UUID organizationId
    );

    Optional<MaintenanceMaterialUsed> findByIdAndMaintenanceRecord_IdAndOrganization_Id(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId
    );
}
