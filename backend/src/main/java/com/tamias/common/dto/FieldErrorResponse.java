package com.tamias.common.dto;

public record FieldErrorResponse(
        String field,
        String message
) {
}
