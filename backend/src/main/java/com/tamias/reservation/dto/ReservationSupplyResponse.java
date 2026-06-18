package com.tamias.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationSupplyResponse(
        UUID id,
        UUID reservationId,
        UUID inventoryItemId,
        String inventoryItemName,
        UUID brandId,
        String brandName,
        String itemType,
        String internalCode,
        String barcode,
        BigDecimal quantity,
        String unit,
        String itemNameSnapshot,
        String internalCodeSnapshot,
        String barcodeSnapshot,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
