package com.tamias.maintenance.detail.mapper;

import com.tamias.catalog.material.entity.Material;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedRequest;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedResponse;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.entity.MaintenanceMaterialUsed;
import com.tamias.maintenance.detail.entity.MaintenanceRecordPerson;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceDetailMapper {

    public MaintenanceRecordPersonResponse toPersonResponse(MaintenanceRecordPerson entity) {
        var person = entity.getMaintenancePerson();

        return new MaintenanceRecordPersonResponse(
                entity.getId(),
                entity.getMaintenanceRecord().getId(),
                person.getId(),
                person.getFullName(),
                person.getPhone(),
                person.getEmail(),
                person.getNotes()
        );
    }

    public void updateMaterialUsed(
            MaintenanceMaterialUsed entity,
            MaintenanceMaterialUsedRequest request,
            Material material,
            String materialNameSnapshot,
            String unit
    ) {
        entity.setMaterial(material);
        entity.setMaterialNameSnapshot(materialNameSnapshot);
        entity.setQuantity(request.quantity());
        entity.setUnit(unit);
        entity.setNotes(request.notes());
    }

    public void updateMaterialUsed(
            MaintenanceMaterialUsed entity,
            MaintenanceMaterialUsedUpdateRequest request,
            Material material,
            String materialNameSnapshot,
            String unit
    ) {
        entity.setMaterial(material);
        entity.setMaterialNameSnapshot(materialNameSnapshot);
        entity.setQuantity(request.quantity());
        entity.setUnit(unit);
        entity.setNotes(request.notes());
    }

    public MaintenanceMaterialUsedResponse toMaterialUsedResponse(MaintenanceMaterialUsed entity) {
        var material = entity.getMaterial();

        return new MaintenanceMaterialUsedResponse(
                entity.getId(),
                entity.getMaintenanceRecord().getId(),
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                entity.getMaterialNameSnapshot(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getNotes()
        );
    }
}
