package com.tamias.purchase.dto;

import jakarta.validation.constraints.NotNull;

public record PurchaseItemPurchasedRequest(
        @NotNull(message = "Purchased is required")
        Boolean purchased
) {
}
