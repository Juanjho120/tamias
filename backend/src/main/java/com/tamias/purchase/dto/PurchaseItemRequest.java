package com.tamias.purchase.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseItemRequest(
        UUID materialId,

        UUID brandId,

        @Size(max = 150, message = "Item name must not exceed 150 characters")
        String itemNameSnapshot,

        @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        @DecimalMin(value = "0.00", message = "Estimated price must be greater than or equal to 0")
        BigDecimal estimatedPrice,

        Boolean purchased,

        String notes
) {
}
