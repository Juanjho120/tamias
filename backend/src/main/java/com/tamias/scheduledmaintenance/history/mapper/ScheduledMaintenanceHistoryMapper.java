package com.tamias.scheduledmaintenance.history.mapper;

import com.tamias.scheduledmaintenance.history.dto.ScheduledMaintenanceHistoryResponse;
import com.tamias.scheduledmaintenance.history.entity.ScheduledMaintenanceHistory;
import org.springframework.stereotype.Component;

@Component
public class ScheduledMaintenanceHistoryMapper {

    public ScheduledMaintenanceHistoryResponse toResponse(ScheduledMaintenanceHistory entity) {
        var changedBy = entity.getChangedBy();

        return new ScheduledMaintenanceHistoryResponse(
                entity.getId(),
                entity.getScheduledMaintenance().getId(),
                entity.getPreviousStatus(),
                entity.getNewStatus(),
                entity.getPreviousPlannedDate(),
                entity.getNewPlannedDate(),
                entity.getPreviousPlannedTime(),
                entity.getNewPlannedTime(),
                entity.getReason(),
                changedBy != null ? changedBy.getId() : null,
                changedBy != null ? changedBy.getFirstName() + " " + changedBy.getLastName() : null,
                entity.getChangedAt()
        );
    }
}
