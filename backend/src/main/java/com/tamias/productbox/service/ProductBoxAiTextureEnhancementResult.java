package com.tamias.productbox.service;

public record ProductBoxAiTextureEnhancementResult(
    byte[] bytes,
    String filename,
    String contentType,
    Integer widthPx,
    Integer heightPx,
    String provider,
    String model,
    String promptVersion
) {
}
