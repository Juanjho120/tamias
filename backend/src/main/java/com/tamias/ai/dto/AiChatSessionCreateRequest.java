package com.tamias.ai.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiChatSessionCreateRequest(
        UUID propertyId,
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title
) {
}
