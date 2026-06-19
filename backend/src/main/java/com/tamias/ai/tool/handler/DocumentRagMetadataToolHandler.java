package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.AiReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(90)
public class DocumentRagMetadataToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public DocumentRagMetadataToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleDocumentAndRagQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleDocumentAndRagQuestion(String question, String normalized) {
        if (isRagChunkSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragChunkSummary());
        }
        if (isRagMissingChunksQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentsMissingChunks());
        }
        if (isRagMissingVectorIdsQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentsMissingVectorIds());
        }
        if (isRagCoverageSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragIndexCoverageSummary());
        }
        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }
        if (isDocumentContentQuestion(normalized)) {
            return Optional.empty();
        }
        if (!isDocumentToolQuestion(normalized)) {
            return Optional.empty();
        }
        if (isDocumentBlueprintQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findBlueprintDocuments());
        }
        if (isDocumentHouseRulesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findHouseRulesDocuments());
        }
        if (isDocumentManualQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findManualDocuments());
        }
        if (isDocumentCountByTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentCountByType());
        }
        if (isDocumentCountByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentCountByProperty());
        }
        if (isDocumentFailedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.failedDocuments());
        }
        if (isDocumentProcessedNotIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.processedNotIndexedDocuments());
        }
        if (isDocumentNotIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.notIndexedDocuments());
        }
        if (isDocumentIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.indexedDocuments());
        }
        if (isDocumentUnprocessedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.unprocessedDocuments());
        }
        if (isDocumentProcessedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.processedDocuments());
        }
        if (isDocumentRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentDocuments());
        }
        if (isDocumentByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByProperty(question));
        }
        if (isDocumentByTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByType(question));
        }
        if (isDocumentByStatusQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByStatus(question));
        }
        return Optional.of(readOnlyToolService.documentMetadata(question));
    }
}
