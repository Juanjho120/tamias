package com.tamias.document.dto;

import com.tamias.document.enums.DocumentProcessingStatus;
import java.util.UUID;

public record DocumentIndexingResponse(
        UUID documentId,
        DocumentProcessingStatus processingStatus,
        int indexedChunks
) {
}
