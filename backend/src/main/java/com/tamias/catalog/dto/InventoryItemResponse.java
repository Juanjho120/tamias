package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        String name,
        String description,
        String unit,
        InventoryItemType itemType,
        String internalCode,
        String barcode,
        Boolean availableForMaintenance,
        Boolean availableForReservations,
        Boolean availableForPurchases,
        CatalogStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
