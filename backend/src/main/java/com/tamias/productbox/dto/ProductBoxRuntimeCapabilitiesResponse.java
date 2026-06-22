package com.tamias.productbox.dto;

public record ProductBoxRuntimeCapabilitiesResponse(
    boolean opencvEnabled,
    boolean aiTextureEnhancementEnabled,
    String opencvDisabledMessage,
    String aiTextureEnhancementDisabledMessage
) {
}
