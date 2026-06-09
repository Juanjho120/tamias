package com.tamias.image.dto;

import com.tamias.image.enums.ImageStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageUploadResponse(
        UUID id,
        UUID parentId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Boolean cover,
        ImageStatus status,
        OffsetDateTime createdAt
) {
}
