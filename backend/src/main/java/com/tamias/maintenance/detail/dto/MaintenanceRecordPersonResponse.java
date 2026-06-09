package com.tamias.maintenance.detail.dto;

import java.util.UUID;

public record MaintenanceRecordPersonResponse(
        UUID id,
        UUID maintenanceRecordId,
        UUID maintenancePersonId,
        String fullName,
        String phone,
        String email,
        String notes
) {
}
