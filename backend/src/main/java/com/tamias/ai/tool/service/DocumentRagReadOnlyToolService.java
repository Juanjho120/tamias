package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.DocumentRagToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentRagReadOnlyToolService {

    private final DocumentRagToolRepository repository;

    public DocumentRagReadOnlyToolService(DocumentRagToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer documentMetadata(String userQuestion) {
        return repository.documentMetadata(userQuestion);
    }

    public AiToolAnswer ragDocumentIndexStatus() {
        return repository.ragDocumentIndexStatus();
    }

    public AiToolAnswer documentByProperty(String userQuestion) {
        return repository.documentByProperty(userQuestion);
    }

    public AiToolAnswer documentByType(String userQuestion) {
        return repository.documentByType(userQuestion);
    }

    public AiToolAnswer documentByStatus(String userQuestion) {
        return repository.documentByStatus(userQuestion);
    }

    public AiToolAnswer recentDocuments() {
        return repository.recentDocuments();
    }

    public AiToolAnswer unprocessedDocuments() {
        return repository.unprocessedDocuments();
    }

    public AiToolAnswer failedDocuments() {
        return repository.failedDocuments();
    }

    public AiToolAnswer processedDocuments() {
        return repository.processedDocuments();
    }

    public AiToolAnswer indexedDocuments() {
        return repository.indexedDocuments();
    }

    public AiToolAnswer notIndexedDocuments() {
        return repository.notIndexedDocuments();
    }

    public AiToolAnswer processedNotIndexedDocuments() {
        return repository.processedNotIndexedDocuments();
    }

    public AiToolAnswer documentCountByType() {
        return repository.documentCountByType();
    }

    public AiToolAnswer documentCountByProperty() {
        return repository.documentCountByProperty();
    }

    public AiToolAnswer findBlueprintDocuments() {
        return repository.findBlueprintDocuments();
    }

    public AiToolAnswer findHouseRulesDocuments() {
        return repository.findHouseRulesDocuments();
    }

    public AiToolAnswer findManualDocuments() {
        return repository.findManualDocuments();
    }

    public AiToolAnswer ragChunkSummary() {
        return repository.ragChunkSummary();
    }

    public AiToolAnswer documentsMissingChunks() {
        return repository.documentsMissingChunks();
    }

    public AiToolAnswer documentsMissingVectorIds() {
        return repository.documentsMissingVectorIds();
    }

    public AiToolAnswer ragIndexCoverageSummary() {
        return repository.ragIndexCoverageSummary();
    }
}
