package com.tamias.dashboard.analytics.dto;

import java.math.BigDecimal;

public record TopItemResponse(
    String name,
    BigDecimal quantity,
    BigDecimal amount
) {
}
