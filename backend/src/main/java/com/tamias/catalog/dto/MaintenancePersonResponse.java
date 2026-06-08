package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaintenancePersonResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        String notes,
        CatalogStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
