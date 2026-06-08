package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String name,
        String phone,
        String email,
        String website,
        String notes,
        CatalogStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
