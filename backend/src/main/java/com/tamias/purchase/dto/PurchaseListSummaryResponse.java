package com.tamias.purchase.dto;

import com.tamias.purchase.enums.PurchaseListStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseListSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID cityId,
        String cityName,
        UUID supplierId,
        String supplierName,
        LocalDate purchaseDate,
        PurchaseListStatus status,
        long totalItems,
        long purchasedItems,
        BigDecimal estimatedTotal,
        OffsetDateTime createdAt
) {
}
