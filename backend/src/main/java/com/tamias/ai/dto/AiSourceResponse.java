package com.tamias.ai.dto;

import java.util.UUID;

public record AiSourceResponse(
        String sourceId,
        String vectorId,
        UUID documentId,
        UUID chunkId,
        UUID propertyId,
        String documentTitle,
        String documentType,
        Integer chunkIndex,
        Double score,
        String excerpt
) {
}
