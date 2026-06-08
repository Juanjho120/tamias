package com.tamias.catalog.dto;

import com.tamias.catalog.enums.CatalogStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        String website,

        String notes,

        @NotNull(message = "Status is required")
        CatalogStatus status
) {
}
