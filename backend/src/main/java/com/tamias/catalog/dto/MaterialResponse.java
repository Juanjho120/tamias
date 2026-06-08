package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String name,
        String description,
        String unit,
        CatalogStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
