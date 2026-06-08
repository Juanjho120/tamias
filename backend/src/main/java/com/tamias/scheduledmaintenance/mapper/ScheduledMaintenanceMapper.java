package com.tamias.scheduledmaintenance.mapper;

import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceSummaryResponse;
import com.tamias.scheduledmaintenance.entity.ScheduledMaintenance;
import org.springframework.stereotype.Component;

@Component
public class ScheduledMaintenanceMapper {

    public void updateEntity(ScheduledMaintenance entity, ScheduledMaintenanceRequest request) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setFrequency(request.frequency());
        entity.setIntervalValue(request.intervalValue());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setNextDueDate(request.nextDueDate() != null ? request.nextDueDate() : request.startDate());
        entity.setEstimatedCost(request.estimatedCost());
        entity.setStatus(request.status());
    }

    public ScheduledMaintenanceSummaryResponse toSummaryResponse(ScheduledMaintenance entity) {
        var property = entity.getProperty();
        var category = entity.getMaintenanceCategory();
        var type = entity.getMaintenanceType();

        return new ScheduledMaintenanceSummaryResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                type != null ? type.getId() : null,
                type != null ? type.getName() : null,
                entity.getTitle(),
                entity.getFrequency(),
                entity.getIntervalValue(),
                entity.getNextDueDate(),
                entity.getEstimatedCost(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public ScheduledMaintenanceResponse toResponse(ScheduledMaintenance entity) {
        var property = entity.getProperty();
        var category = entity.getMaintenanceCategory();
        var type = entity.getMaintenanceType();
        var person = entity.getMaintenancePerson();

        return new ScheduledMaintenanceResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                type != null ? type.getId() : null,
                type != null ? type.getName() : null,
                person != null ? person.getId() : null,
                person != null ? person.getFullName() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getFrequency(),
                entity.getIntervalValue(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getNextDueDate(),
                entity.getLastGeneratedAt(),
                entity.getEstimatedCost(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
