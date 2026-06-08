package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CityRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @NotNull(message = "Status is required")
        CatalogStatus status
) {
}
