package com.tamias.document.processing;

import com.tamias.document.dto.DocumentProcessingResponse;
import com.tamias.document.entity.Document;
import com.tamias.document.entity.DocumentChunk;
import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.repository.DocumentChunkRepository;
import com.tamias.document.repository.DocumentRepository;
import com.tamias.document.storage.FileStorageService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);
    private static final String VECTOR_STORE_COLLECTION = "tamias_documents";

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final FileStorageService fileStorageService;
    private final TextExtractionServiceFactory textExtractionServiceFactory;
    private final ChunkingService chunkingService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            FileStorageService fileStorageService,
            TextExtractionServiceFactory textExtractionServiceFactory,
            ChunkingService chunkingService
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.fileStorageService = fileStorageService;
        this.textExtractionServiceFactory = textExtractionServiceFactory;
        this.chunkingService = chunkingService;
    }

    @Transactional
    public DocumentProcessingResponse process(Document document) {
        log.info("Starting document processing. documentId={}, organizationId={}",
                document.getId(),
                document.getOrganization().getId()
        );

        document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);
        documentRepository.saveAndFlush(document);

        try {
            Resource resource = fileStorageService.loadAsResource(document.getS3Key());
            TextExtractionService extractor = textExtractionServiceFactory.getExtractor(document);
            ExtractedText extractedText = extractor.extract(document, resource);

            documentChunkRepository.deleteByDocument_Id(document.getId());

            List<TextChunk> chunks = chunkingService.split(extractedText.content());

            if (chunks.isEmpty()) {
                document.setProcessingStatus(DocumentProcessingStatus.FAILED);
                documentRepository.save(document);
                log.warn("Document processing failed because no text was extracted. documentId={}", document.getId());
                return new DocumentProcessingResponse(document.getId(), document.getProcessingStatus());
            }

            for (TextChunk textChunk : chunks) {
                DocumentChunk documentChunk = new DocumentChunk();
                documentChunk.setOrganization(document.getOrganization());
                documentChunk.setDocument(document);
                documentChunk.setChunkIndex(textChunk.index());
                documentChunk.setContent(textChunk.content());
                documentChunk.setTokenCount(textChunk.tokenCount());
                documentChunk.setVectorStoreCollection(VECTOR_STORE_COLLECTION);
                documentChunk.setVectorStoreId(null);

                documentChunkRepository.save(documentChunk);
            }

            document.setProcessingStatus(DocumentProcessingStatus.PROCESSED);
            documentRepository.save(document);
            log.info("Document processing completed. documentId={}, chunks={}", document.getId(), chunks.size());
            return new DocumentProcessingResponse(document.getId(), document.getProcessingStatus());
        } catch (Exception exception) {
            log.warn("Document processing failed. documentId={}, reason={}", document.getId(), exception.getMessage());
            document.setProcessingStatus(DocumentProcessingStatus.FAILED);
            documentRepository.save(document);
            return new DocumentProcessingResponse(document.getId(), document.getProcessingStatus());
        }
    }
}
