package com.tamias.productbox.dto;

import com.tamias.productbox.enums.ProductBoxUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductBoxModelRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    String name,

    String description,

    UUID inventoryItemId,

    UUID purchaseItemId,

    @NotNull(message = "Width is required")
    @DecimalMin(value = "0.01", message = "Width must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Width must have up to 8 integer digits and 2 decimals")
    BigDecimal width,

    @NotNull(message = "Height is required")
    @DecimalMin(value = "0.01", message = "Height must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Height must have up to 8 integer digits and 2 decimals")
    BigDecimal height,

    @NotNull(message = "Depth is required")
    @DecimalMin(value = "0.01", message = "Depth must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Depth must have up to 8 integer digits and 2 decimals")
    BigDecimal depth,

    @NotNull(message = "Unit is required")
    ProductBoxUnit unit
) {
}
