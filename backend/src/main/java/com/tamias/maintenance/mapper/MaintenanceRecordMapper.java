package com.tamias.maintenance.mapper;

import com.tamias.maintenance.dto.MaintenanceRecordRequest;
import com.tamias.maintenance.dto.MaintenanceRecordResponse;
import com.tamias.maintenance.dto.MaintenanceRecordSummaryResponse;
import com.tamias.maintenance.entity.MaintenanceRecord;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceRecordMapper {

    public void updateEntity(MaintenanceRecord entity, MaintenanceRecordRequest request) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setScheduledAt(request.scheduledAt());
        entity.setPerformedAt(request.performedAt());
        entity.setCost(request.cost());
        entity.setStatus(request.status());
    }

    public MaintenanceRecordSummaryResponse toSummaryResponse(MaintenanceRecord entity) {
        var property = entity.getProperty();
        var maintenanceCategory = entity.getMaintenanceCategory();
        var maintenanceType = entity.getMaintenanceType();

        return new MaintenanceRecordSummaryResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                maintenanceCategory != null ? maintenanceCategory.getId() : null,
                maintenanceCategory != null ? maintenanceCategory.getName() : null,
                maintenanceType != null ? maintenanceType.getId() : null,
                maintenanceType != null ? maintenanceType.getName() : null,
                entity.getTitle(),
                entity.getScheduledAt(),
                entity.getPerformedAt(),
                entity.getCost(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public MaintenanceRecordResponse toResponse(MaintenanceRecord entity) {
        var property = entity.getProperty();
        var maintenanceCategory = entity.getMaintenanceCategory();
        var maintenanceType = entity.getMaintenanceType();
        var maintenancePerson = entity.getMaintenancePerson();

        return new MaintenanceRecordResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                maintenanceCategory != null ? maintenanceCategory.getId() : null,
                maintenanceCategory != null ? maintenanceCategory.getName() : null,
                maintenanceType != null ? maintenanceType.getId() : null,
                maintenanceType != null ? maintenanceType.getName() : null,
                maintenancePerson != null ? maintenancePerson.getId() : null,
                maintenancePerson != null ? maintenancePerson.getFullName() : null,
                entity.getTitle(),
                entity.getDescription(),
                entity.getScheduledAt(),
                entity.getPerformedAt(),
                entity.getCost(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
