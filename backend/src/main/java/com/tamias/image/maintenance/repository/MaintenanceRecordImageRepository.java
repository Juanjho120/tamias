package com.tamias.image.maintenance.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.maintenance.entity.MaintenanceRecordImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordImageRepository extends JpaRepository<MaintenanceRecordImage, UUID> {

    List<MaintenanceRecordImage> findByMaintenanceRecord_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID maintenanceRecordId,
            UUID organizationId,
            ImageStatus status
    );

    Optional<MaintenanceRecordImage> findByIdAndMaintenanceRecord_IdAndOrganization_IdAndStatus(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId,
            ImageStatus status
    );
}
