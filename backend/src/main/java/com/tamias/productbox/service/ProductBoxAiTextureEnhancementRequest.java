package com.tamias.productbox.service;

import com.tamias.productbox.enums.ProductBoxFaceName;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductBoxAiTextureEnhancementRequest(
    UUID organizationId,
    UUID productBoxModelId,
    UUID faceId,
    ProductBoxFaceName faceName,
    String processedTextureKey,
    String processedTextureFilename,
    String processedTextureContentType,
    byte[] processedTextureBytes,
    BigDecimal targetAspectRatio,
    Integer processedWidthPx,
    Integer processedHeightPx,
    String promptVersion
) {
}
