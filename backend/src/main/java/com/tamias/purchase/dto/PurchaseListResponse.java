package com.tamias.purchase.dto;

import com.tamias.purchase.enums.PurchaseListStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseListResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID cityId,
        String cityName,
        UUID supplierId,
        String supplierName,
        LocalDate purchaseDate,
        String notes,
        PurchaseListStatus status,
        List<PurchaseItemResponse> items,
        BigDecimal estimatedTotal,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
