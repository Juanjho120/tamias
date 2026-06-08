package com.tamias.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record AiSearchRequest(
        @NotBlank(message = "Question is required")
        String question,

        UUID propertyId,

        @Min(value = 1, message = "topK must be greater than or equal to 1")
        @Max(value = 20, message = "topK must be less than or equal to 20")
        Integer topK,

        Double similarityThreshold
) {
}
