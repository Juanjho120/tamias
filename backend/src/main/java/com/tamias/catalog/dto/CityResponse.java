package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CityResponse(
        UUID id,
        String name,
        String description,
        String department,
        String country,
        CatalogStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
