package com.tamias.productbox.dto;

import com.tamias.productbox.enums.ProductBoxTextureStatus;
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
    Integer imageUrlExpiresIn,
    String originalImageKey,
    String originalFilepath,
    String originalUploadFilename,
    String originalContentType,
    Long originalSizeBytes,
    Integer originalWidthPx,
    Integer originalHeightPx,
    String originalImageUrl,
    String processedImageKey,
    String processedFilepath,
    String processedFilename,
    String processedContentType,
    Long processedSizeBytes,
    Integer processedWidthPx,
    Integer processedHeightPx,
    String processedImageUrl,
    BigDecimal targetAspectRatio,
    String pointsJson,
    ProductBoxTextureStatus textureStatus,
    String processingError,
    OffsetDateTime processedAt,
    OffsetDateTime acceptedAt
) { }
