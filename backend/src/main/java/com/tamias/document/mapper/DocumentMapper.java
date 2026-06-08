package com.tamias.document.mapper;

import com.tamias.document.dto.DocumentChunkResponse;
import com.tamias.document.dto.DocumentResponse;
import com.tamias.document.dto.DocumentSummaryResponse;
import com.tamias.document.entity.Document;
import com.tamias.document.entity.DocumentChunk;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentSummaryResponse toSummaryResponse(Document entity) {
        var property = entity.getProperty();

        return new DocumentSummaryResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                entity.getDocumentType(),
                entity.getTitle(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getProcessingStatus(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public DocumentResponse toResponse(Document entity) {
        var property = entity.getProperty();

        return new DocumentResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                entity.getDocumentType(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getOriginalFilename(),
                entity.getS3Key(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getProcessingStatus(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DocumentChunkResponse toChunkResponse(DocumentChunk entity) {
        return new DocumentChunkResponse(
                entity.getId(),
                entity.getChunkIndex(),
                entity.getContent(),
                entity.getTokenCount(),
                entity.getVectorStoreCollection(),
                entity.getVectorStoreId()
        );
    }
}
