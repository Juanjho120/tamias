package com.tamias.maintenance.detail.mapper;

import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.entity.MaintenanceRecordItem;
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

    public void updateRecordItem(
            MaintenanceRecordItem entity,
            MaintenanceRecordItemRequest request,
            InventoryItem inventoryItem,
            String itemNameSnapshot,
            String unit
    ) {
        entity.setInventoryItem(inventoryItem);
        entity.setItemNameSnapshot(itemNameSnapshot);
        entity.setQuantity(request.quantity());
        entity.setUnit(unit);
        entity.setNotes(request.notes());
    }

    public void updateRecordItem(
            MaintenanceRecordItem entity,
            MaintenanceRecordItemUpdateRequest request,
            InventoryItem inventoryItem,
            String itemNameSnapshot,
            String unit
    ) {
        entity.setInventoryItem(inventoryItem);
        entity.setItemNameSnapshot(itemNameSnapshot);
        entity.setQuantity(request.quantity());
        entity.setUnit(unit);
        entity.setNotes(request.notes());
    }

    public MaintenanceRecordItemResponse toRecordItemResponse(MaintenanceRecordItem entity) {
        var inventoryItem = entity.getInventoryItem();

        return new MaintenanceRecordItemResponse(
                entity.getId(),
                entity.getMaintenanceRecord().getId(),
                inventoryItem != null ? inventoryItem.getId() : null,
                inventoryItem != null ? inventoryItem.getName() : null,
                inventoryItem != null ? inventoryItem.getId() : null,
                inventoryItem != null ? inventoryItem.getName() : null,
                entity.getItemNameSnapshot(),
                entity.getItemNameSnapshot(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getNotes()
        );
    }
}
