package com.tamias.dashboard.analytics.dto;

import java.math.BigDecimal;

public record MonthlyAmountResponse(
    String month,
    BigDecimal amount
) {
}
