package com.tamias.document.dto;

import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.enums.DocumentStatus;
import com.tamias.document.enums.DocumentType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        DocumentType documentType,
        String title,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        DocumentProcessingStatus processingStatus,
        DocumentStatus status,
        OffsetDateTime createdAt
) {
}
