package com.tamias.productbox.service;

public interface ProductBoxAiTextureEnhancementProvider {

    String getProviderName();

    boolean isAvailable();

    ProductBoxAiTextureEnhancementResult enhance(ProductBoxAiTextureEnhancementRequest request);
}
