package com.tamias.document.dto;

import com.tamias.document.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DocumentUploadRequest(
        UUID propertyId,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        String description
) {
}
