package com.tamias.property.dto;

import com.tamias.property.enums.PropertyStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String name,
        String address,
        String description,
        PropertyStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
