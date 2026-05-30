package com.tamias.property.dto;

import com.tamias.property.enums.PropertyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        String address,

        String description,

        @NotNull(message = "Status is required")
        PropertyStatus status
) {
}
