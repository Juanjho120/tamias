package com.tamias.organization.dto;

import com.tamias.organization.enums.OrganizationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String description,
        OrganizationStatus status,
        String logoUrl,
        String logoOriginalFilename,
        String logoContentType,
        Long logoSizeBytes,
        OffsetDateTime logoUpdatedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
