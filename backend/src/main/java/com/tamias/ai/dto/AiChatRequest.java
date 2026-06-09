package com.tamias.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record AiChatRequest(
        UUID chatSessionId,
        UUID propertyId,
        String title,
        @NotBlank(message = "Question is required")
        String question,
        @Min(value = 1, message = "topK must be greater than or equal to 1")
        @Max(value = 20, message = "topK must be less than or equal to 20")
        Integer topK,
        Double similarityThreshold
) {
}
