package com.tamias.maintenance.detail.repository;

import com.tamias.maintenance.detail.entity.MaintenanceRecordItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordItemRepository extends JpaRepository<MaintenanceRecordItem, UUID> {

    List<MaintenanceRecordItem> findByMaintenanceRecord_IdAndOrganization_IdOrderByIdAsc(
            UUID maintenanceRecordId,
            UUID organizationId
    );

    Optional<MaintenanceRecordItem> findByIdAndMaintenanceRecord_IdAndOrganization_Id(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId
    );
}
