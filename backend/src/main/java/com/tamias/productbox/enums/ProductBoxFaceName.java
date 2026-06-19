package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxFaceName {
    FRONT("front"),
    BACK("back"),
    LEFT("left"),
    RIGHT("right"),
    TOP("top"),
    BOTTOM("bottom");

    private final String value;

    ProductBoxFaceName(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxFaceName fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Product box face name is required");
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(face -> face.value.equalsIgnoreCase(normalizedValue) || face.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box face name"));
    }
}
