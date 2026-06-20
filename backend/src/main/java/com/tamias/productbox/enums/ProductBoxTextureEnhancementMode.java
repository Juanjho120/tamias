package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxTextureEnhancementMode {
    NONE("none"),
    BASIC("basic"),
    STRONG("strong");

    private final String value;

    ProductBoxTextureEnhancementMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxTextureEnhancementMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return BASIC;
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(mode -> mode.value.equalsIgnoreCase(normalizedValue) || mode.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box texture enhancement mode"));
    }
}
