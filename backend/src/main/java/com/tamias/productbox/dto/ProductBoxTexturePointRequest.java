package com.tamias.productbox.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductBoxTexturePointRequest(
    @NotNull(message = "Point x coordinate is required")
    @DecimalMin(value = "0.0", message = "Point x coordinate must be greater than or equal to 0")
    BigDecimal x,

    @NotNull(message = "Point y coordinate is required")
    @DecimalMin(value = "0.0", message = "Point y coordinate must be greater than or equal to 0")
    BigDecimal y
) { }
