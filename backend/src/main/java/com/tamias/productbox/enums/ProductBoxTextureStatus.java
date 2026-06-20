package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxTextureStatus {
    UPLOADED("UPLOADED"),
    POINTS_SELECTED("POINTS_SELECTED"),
    PROCESSED("PROCESSED"),
    ACCEPTED("ACCEPTED"),
    FAILED("FAILED");

    private final String value;

    ProductBoxTextureStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxTextureStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Product box texture status is required");
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(status -> status.value.equalsIgnoreCase(normalizedValue) || status.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box texture status"));
    }
}
