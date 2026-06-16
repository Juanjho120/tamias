package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DocumentRagReadOnlyToolService extends AiReadOnlyToolSupport {

    public DocumentRagReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer documentMetadata(String userQuestion) {
        return super.documentMetadata(userQuestion);
    }

    public AiToolAnswer ragDocumentIndexStatus() {
        return super.ragDocumentIndexStatus();
    }

    public AiToolAnswer documentByProperty(String userQuestion) {
        return super.documentByProperty(userQuestion);
    }

    public AiToolAnswer documentByType(String userQuestion) {
        return super.documentByType(userQuestion);
    }

    public AiToolAnswer documentByStatus(String userQuestion) {
        return super.documentByStatus(userQuestion);
    }

    public AiToolAnswer recentDocuments() {
        return super.recentDocuments();
    }

    public AiToolAnswer unprocessedDocuments() {
        return super.unprocessedDocuments();
    }

    public AiToolAnswer failedDocuments() {
        return super.failedDocuments();
    }

    public AiToolAnswer processedDocuments() {
        return super.processedDocuments();
    }

    public AiToolAnswer indexedDocuments() {
        return super.indexedDocuments();
    }

    public AiToolAnswer notIndexedDocuments() {
        return super.notIndexedDocuments();
    }

    public AiToolAnswer processedNotIndexedDocuments() {
        return super.processedNotIndexedDocuments();
    }

    public AiToolAnswer documentCountByType() {
        return super.documentCountByType();
    }

    public AiToolAnswer documentCountByProperty() {
        return super.documentCountByProperty();
    }

    public AiToolAnswer findBlueprintDocuments() {
        return super.findBlueprintDocuments();
    }

    public AiToolAnswer findHouseRulesDocuments() {
        return super.findHouseRulesDocuments();
    }

    public AiToolAnswer findManualDocuments() {
        return super.findManualDocuments();
    }

    public AiToolAnswer ragChunkSummary() {
        return super.ragChunkSummary();
    }

    public AiToolAnswer documentsMissingChunks() {
        return super.documentsMissingChunks();
    }

    public AiToolAnswer documentsMissingVectorIds() {
        return super.documentsMissingVectorIds();
    }

    public AiToolAnswer ragIndexCoverageSummary() {
        return super.ragIndexCoverageSummary();
    }

}
