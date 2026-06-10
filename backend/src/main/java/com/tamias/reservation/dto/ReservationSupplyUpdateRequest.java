package com.tamias.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ReservationSupplyUpdateRequest(
        @NotNull(message = "Inventory item is required")
        UUID inventoryItemId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        String notes
) {
}
