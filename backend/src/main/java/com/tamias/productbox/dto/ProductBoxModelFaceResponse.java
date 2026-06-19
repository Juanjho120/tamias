package com.tamias.productbox.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductBoxModelFaceResponse(
    UUID id,
    String faceName,
    String imageKey,
    String filepath,
    String originalFilename,
    String contentType,
    Long sizeBytes,
    BigDecimal rotationDegrees,
    Boolean flipHorizontal,
    Boolean flipVertical,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String imageUrl,
    Integer imageUrlExpiresIn
) {
}
