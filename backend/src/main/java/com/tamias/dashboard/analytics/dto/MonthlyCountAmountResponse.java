package com.tamias.dashboard.analytics.dto;

import java.math.BigDecimal;

public record MonthlyCountAmountResponse(
    String month,
    long count,
    BigDecimal amount
) {
}
