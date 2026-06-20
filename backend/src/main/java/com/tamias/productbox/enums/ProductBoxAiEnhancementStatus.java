package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxAiEnhancementStatus {
    NOT_REQUESTED("NOT_REQUESTED"),
    REQUESTED("REQUESTED"),
    PROCESSING("PROCESSING"),
    GENERATED("GENERATED"),
    ACCEPTED("ACCEPTED"),
    FAILED("FAILED");

    private final String value;

    ProductBoxAiEnhancementStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxAiEnhancementStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NOT_REQUESTED;
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(status -> status.value.equalsIgnoreCase(normalizedValue) || status.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box AI enhancement status"));
    }
}
