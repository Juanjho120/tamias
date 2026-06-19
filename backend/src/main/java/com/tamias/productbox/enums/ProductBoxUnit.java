package com.tamias.productbox.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tamias.common.exception.BadRequestException;
import java.util.Arrays;

public enum ProductBoxUnit {
    CM("cm"),
    MM("mm"),
    IN("in");

    private final String value;

    ProductBoxUnit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProductBoxUnit fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
            .filter(unit -> unit.value.equalsIgnoreCase(normalizedValue) || unit.name().equalsIgnoreCase(normalizedValue))
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Invalid product box unit"));
    }
}
