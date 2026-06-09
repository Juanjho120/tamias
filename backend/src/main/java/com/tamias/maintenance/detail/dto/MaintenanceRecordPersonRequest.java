package com.tamias.maintenance.detail.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MaintenanceRecordPersonRequest(
        @NotNull(message = "Maintenance person is required")
        UUID maintenancePersonId
) {
}
