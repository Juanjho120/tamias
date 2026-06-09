package com.tamias.maintenance.detail.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record MaintenanceMaterialUsedRequest(
        UUID materialId,

        @Size(max = 150, message = "Material name must not exceed 150 characters")
        String materialNameSnapshot,

        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        String notes
) {
}
