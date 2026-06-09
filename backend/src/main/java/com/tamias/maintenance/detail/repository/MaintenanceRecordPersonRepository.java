package com.tamias.maintenance.detail.repository;

import com.tamias.maintenance.detail.entity.MaintenanceRecordPerson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordPersonRepository extends JpaRepository<MaintenanceRecordPerson, UUID> {

    List<MaintenanceRecordPerson> findByMaintenanceRecord_IdAndOrganization_Id(
            UUID maintenanceRecordId,
            UUID organizationId
    );

    Optional<MaintenanceRecordPerson> findByIdAndMaintenanceRecord_IdAndOrganization_Id(
            UUID id,
            UUID maintenanceRecordId,
            UUID organizationId
    );

    boolean existsByMaintenanceRecord_IdAndMaintenancePerson_IdAndOrganization_Id(
            UUID maintenanceRecordId,
            UUID maintenancePersonId,
            UUID organizationId
    );
}
