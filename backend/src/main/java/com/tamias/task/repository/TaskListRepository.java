package com.tamias.task.repository;

import com.tamias.task.entity.TaskList;
import com.tamias.task.enums.TaskListStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskListRepository extends JpaRepository<TaskList, UUID> {

    Optional<TaskList> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<TaskList> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<TaskList> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            TaskListStatus status,
            Pageable pageable
    );

    Page<TaskList> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<TaskList> findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            TaskListStatus status,
            Pageable pageable
    );

    Page<TaskList> findByOrganization_IdAndReservation_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID reservationId,
            Pageable pageable
    );

    Page<TaskList> findByOrganization_IdAndMaintenanceRecord_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID maintenanceRecordId,
            Pageable pageable
    );
}
