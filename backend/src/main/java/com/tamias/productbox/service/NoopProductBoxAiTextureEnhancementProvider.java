package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class NoopProductBoxAiTextureEnhancementProvider implements ProductBoxAiTextureEnhancementProvider {

    @Override
    public String getProviderName() {
        return "noop";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public ProductBoxAiTextureEnhancementResult enhance(ProductBoxAiTextureEnhancementRequest request) {
        throw new BadRequestException("Product box AI texture enhancement provider is not configured");
    }
}
