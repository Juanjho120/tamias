package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskTemplateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        String description,

        @NotNull(message = "Status is required")
        CatalogStatus status
) {
}
