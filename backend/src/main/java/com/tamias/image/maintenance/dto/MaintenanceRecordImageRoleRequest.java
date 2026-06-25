package com.tamias.image.maintenance.dto;

import com.tamias.image.maintenance.enums.MaintenanceImageRole;
import jakarta.validation.constraints.NotNull;

public record MaintenanceRecordImageRoleRequest(
        @NotNull(message = "Image role is required") MaintenanceImageRole imageRole
) {
}
