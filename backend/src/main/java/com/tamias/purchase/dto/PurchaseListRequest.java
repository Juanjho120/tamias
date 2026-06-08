package com.tamias.purchase.dto;

import com.tamias.purchase.enums.PurchaseListStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseListRequest(
        UUID propertyId,

        UUID cityId,

        UUID supplierId,

        @NotNull(message = "Purchase date is required")
        LocalDate purchaseDate,

        String notes,

        @NotNull(message = "Status is required")
        PurchaseListStatus status,

        @Valid
        List<PurchaseItemRequest> items
) {
}
