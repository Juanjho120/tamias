package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenanceTypeResponse(
        UUID id,
        String name,
        String description,
        CatalogStatus status,
        UUID maintenanceCategoryId,
        String maintenanceCategoryName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
