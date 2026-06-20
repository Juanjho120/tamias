package com.tamias.productbox.dto;

import com.tamias.productbox.enums.ProductBoxTextureEnhancementMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProductBoxTextureProcessRequest(
    @Valid @NotNull(message = "Top-left point is required") ProductBoxTexturePointRequest topLeft,
    @Valid @NotNull(message = "Top-right point is required") ProductBoxTexturePointRequest topRight,
    @Valid @NotNull(message = "Bottom-right point is required") ProductBoxTexturePointRequest bottomRight,
    @Valid @NotNull(message = "Bottom-left point is required") ProductBoxTexturePointRequest bottomLeft,
    ProductBoxTextureEnhancementMode enhancementMode
) {
    public ProductBoxTextureProcessRequest(
        ProductBoxTexturePointRequest topLeft,
        ProductBoxTexturePointRequest topRight,
        ProductBoxTexturePointRequest bottomRight,
        ProductBoxTexturePointRequest bottomLeft
    ) {
        this(topLeft, topRight, bottomRight, bottomLeft, null);
    }
}
