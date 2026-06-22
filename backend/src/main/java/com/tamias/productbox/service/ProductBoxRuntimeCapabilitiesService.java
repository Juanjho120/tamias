package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.productbox.dto.ProductBoxRuntimeCapabilitiesResponse;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ProductBoxRuntimeCapabilitiesService {

    public static final String AI_TEXTURE_DISABLED_MESSAGE =
            "Product Box AI texture enhancement is disabled or not configured in this environment.";

    private final ProductBoxOpenCvRuntimeService openCvRuntimeService;
    private final List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders;

    public ProductBoxRuntimeCapabilitiesService(
            ProductBoxOpenCvRuntimeService openCvRuntimeService,
            List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders
    ) {
        this.openCvRuntimeService = openCvRuntimeService;
        this.aiTextureEnhancementProviders = aiTextureEnhancementProviders != null
                ? aiTextureEnhancementProviders
                : List.of();
    }

    public ProductBoxRuntimeCapabilitiesResponse getCapabilities() {
        boolean openCvEnabled = isOpenCvEnabled();
        boolean aiTextureEnhancementEnabled = isAiTextureEnhancementEnabled();

        return new ProductBoxRuntimeCapabilitiesResponse(
                openCvEnabled,
                aiTextureEnhancementEnabled,
                openCvEnabled ? null : openCvRuntimeService.getDisabledMessage(),
                aiTextureEnhancementEnabled ? null : AI_TEXTURE_DISABLED_MESSAGE
        );
    }

    public void requireOpenCvEnabled() {
        openCvRuntimeService.requireAvailable();
    }

    public void requireAiTextureEnhancementEnabled() {
        if (!isAiTextureEnhancementEnabled()) {
            throw new BadRequestException(AI_TEXTURE_DISABLED_MESSAGE);
        }
    }

    public boolean isOpenCvEnabled() {
        return openCvRuntimeService.isAvailable();
    }

    public boolean isAiTextureEnhancementEnabled() {
        return aiTextureEnhancementProviders.stream()
                .anyMatch(ProductBoxAiTextureEnhancementProvider::isAvailable);
    }
}
