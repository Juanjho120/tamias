package com.tamias.scheduledmaintenance.history.repository;

import com.tamias.scheduledmaintenance.history.entity.ScheduledMaintenanceHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledMaintenanceHistoryRepository extends JpaRepository<ScheduledMaintenanceHistory, UUID> {

    List<ScheduledMaintenanceHistory> findByScheduledMaintenance_IdAndOrganization_IdOrderByChangedAtDesc(
            UUID scheduledMaintenanceId,
            UUID organizationId
    );
}
