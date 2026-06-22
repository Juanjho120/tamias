package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.productbox.dto.ProductBoxRuntimeCapabilitiesResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ProductBoxRuntimeCapabilitiesService {

    public static final String OPENCV_DISABLED_MESSAGE = "Product Box OpenCV texture processing is disabled in this environment.";
    public static final String AI_TEXTURE_DISABLED_MESSAGE = "Product Box AI texture enhancement is disabled or not configured in this environment.";

    private final boolean opencvEnabled;
    private final List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders;

    public ProductBoxRuntimeCapabilitiesService(
        @Value("${tamias.product-box.opencv.enabled:true}") boolean opencvEnabled,
        List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders
    ) {
        this.opencvEnabled = opencvEnabled;
        this.aiTextureEnhancementProviders = aiTextureEnhancementProviders != null ? aiTextureEnhancementProviders : List.of();
    }

    public ProductBoxRuntimeCapabilitiesResponse getCapabilities() {
        return new ProductBoxRuntimeCapabilitiesResponse(
            opencvEnabled,
            isAiTextureEnhancementEnabled(),
            opencvEnabled ? null : OPENCV_DISABLED_MESSAGE,
            isAiTextureEnhancementEnabled() ? null : AI_TEXTURE_DISABLED_MESSAGE
        );
    }

    public void requireOpenCvEnabled() {
        if (!opencvEnabled) {
            throw new BadRequestException(OPENCV_DISABLED_MESSAGE);
        }
    }

    public void requireAiTextureEnhancementEnabled() {
        if (!isAiTextureEnhancementEnabled()) {
            throw new BadRequestException(AI_TEXTURE_DISABLED_MESSAGE);
        }
    }

    public boolean isOpenCvEnabled() {
        return opencvEnabled;
    }

    public boolean isAiTextureEnhancementEnabled() {
        return aiTextureEnhancementProviders.stream().anyMatch(ProductBoxAiTextureEnhancementProvider::isAvailable);
    }
}
