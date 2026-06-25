package com.tamias.maintenance.detail.repository;

import com.tamias.maintenance.detail.entity.MaintenanceRecordServicedItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordServicedItemRepository extends JpaRepository<MaintenanceRecordServicedItem, UUID> {

    List<MaintenanceRecordServicedItem> findByMaintenanceRecord_IdAndOrganization_IdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID maintenanceRecordId,
            UUID organizationId
    );

    Optional<MaintenanceRecordServicedItem> findByIdAndMaintenanceRecord_IdAndOrganization_IdAndDeletedAtIsNull(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId
    );
}
