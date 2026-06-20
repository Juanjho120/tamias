package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxActiveTextureSource {
    UNKNOWN("unknown"),
    DIRECT_UPLOAD("direct_upload"),
    OPENCV_PROCESSED("opencv_processed"),
    AI_ENHANCED("ai_enhanced");

    private final String value;

    ProductBoxActiveTextureSource(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxActiveTextureSource fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(source -> source.value.equalsIgnoreCase(normalizedValue) || source.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box active texture source"));
    }
}
