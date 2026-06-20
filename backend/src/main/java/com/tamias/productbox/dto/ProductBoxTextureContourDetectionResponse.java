package com.tamias.productbox.dto;

import java.math.BigDecimal;

public record ProductBoxTextureContourDetectionResponse(
    boolean detected,
    BigDecimal confidence,
    ProductBoxTextureProcessRequest points,
    String message
) {
}
