package com.tamias.ai.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.document.dto.DocumentIndexingResponse;
import com.tamias.document.entity.Document;
import com.tamias.document.entity.DocumentChunk;
import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.repository.DocumentChunkRepository;
import com.tamias.document.repository.DocumentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RagVectorStoreService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    public RagVectorStoreService(
            VectorStore vectorStore,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository
    ) {
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional
    public DocumentIndexingResponse indexDocument(Document document) {
        if (document.getProcessingStatus() != DocumentProcessingStatus.PROCESSED) {
            throw new BadRequestException("Only processed documents can be indexed");
        }

        List<DocumentChunk> chunks = documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId());

        if (chunks.isEmpty()) {
            throw new BadRequestException("Document has no chunks to index");
        }

        List<String> existingVectorIds = chunks.stream()
                .map(DocumentChunk::getVectorStoreId)
                .filter(vectorStoreId -> vectorStoreId != null && !vectorStoreId.isBlank())
                .toList();

        if (!existingVectorIds.isEmpty()) {
            vectorStore.delete(existingVectorIds);
        }

        List<org.springframework.ai.document.Document> springAiDocuments = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            String vectorId = chunk.getId().toString();

            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put(RagMetadataKeys.ORGANIZATION_ID, document.getOrganization().getId().toString());
            metadata.put(RagMetadataKeys.DOCUMENT_ID, document.getId().toString());
            metadata.put(RagMetadataKeys.DOCUMENT_TITLE, document.getTitle());
            metadata.put(RagMetadataKeys.DOCUMENT_TYPE, document.getDocumentType().name());
            metadata.put(RagMetadataKeys.CHUNK_ID, chunk.getId().toString());
            metadata.put(RagMetadataKeys.CHUNK_INDEX, chunk.getChunkIndex());

            if (document.getProperty() != null) {
                metadata.put(RagMetadataKeys.PROPERTY_ID, document.getProperty().getId().toString());
            } else {
                metadata.put(RagMetadataKeys.PROPERTY_ID, "");
            }

            springAiDocuments.add(new org.springframework.ai.document.Document(
                    vectorId,
                    chunk.getContent(),
                    metadata
            ));

            chunk.setVectorStoreCollection("tamias_documents");
            chunk.setVectorStoreId(vectorId);
        }

        vectorStore.add(springAiDocuments);
        documentChunkRepository.saveAll(chunks);
        documentRepository.save(document);

        return new DocumentIndexingResponse(
                document.getId(),
                document.getProcessingStatus(),
                chunks.size()
        );
    }

    @Transactional
    public void deleteDocumentVectors(Document document) {
        List<String> vectorIds = documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId())
                .stream()
                .map(DocumentChunk::getVectorStoreId)
                .filter(vectorStoreId -> vectorStoreId != null && !vectorStoreId.isBlank())
                .toList();

        if (!vectorIds.isEmpty()) {
            vectorStore.delete(vectorIds);
        }
    }
}
