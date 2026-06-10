package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InventoryItemRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        String description,

        @Size(max = 50, message = "Unit must not exceed 50 characters")
        String unit,

        InventoryItemType itemType,

        @Size(max = 100, message = "Internal code must not exceed 100 characters")
        String internalCode,

        @Size(max = 100, message = "Barcode must not exceed 100 characters")
        String barcode,

        Boolean availableForMaintenance,

        Boolean availableForReservations,

        Boolean availableForPurchases,

        CatalogStatus status
) {
}
