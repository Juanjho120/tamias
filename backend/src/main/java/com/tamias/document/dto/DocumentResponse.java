package com.tamias.document.dto;

import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.enums.DocumentStatus;
import com.tamias.document.enums.DocumentType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        DocumentType documentType,
        String title,
        String description,
        String originalFilename,
        String s3Key,
        String contentType,
        Long sizeBytes,
        DocumentProcessingStatus processingStatus,
        DocumentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String downloadUrl,
        Integer downloadUrlExpiresIn
) {
}
