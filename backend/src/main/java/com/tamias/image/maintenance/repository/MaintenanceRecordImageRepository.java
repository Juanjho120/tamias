package com.tamias.image.maintenance.repository;

import com.tamias.image.maintenance.entity.MaintenanceRecordImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordImageRepository extends JpaRepository<MaintenanceRecordImage, UUID> {

    List<MaintenanceRecordImage> findByMaintenanceRecord_IdAndOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID maintenanceRecordId,
            UUID organizationId
    );

    Optional<MaintenanceRecordImage> findByIdAndMaintenanceRecord_IdAndOrganization_IdAndDeletedAtIsNull(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId
    );
}
