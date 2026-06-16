package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(160)
public class LegacyFallbackToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public LegacyFallbackToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        String question = context.question();
        String normalized = context.normalizedQuestion();

        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }
        if (isDocumentMetadataQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentMetadata(question));
        }
        if (isOperationalSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.operationalSummary());
        }
        if (isUpcomingReservationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.upcomingReservations());
        }
        if (isLastMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPerformedMaintenance(question));
        }
        if (isOverdueScheduledMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.overdueScheduledMaintenance());
        }
        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }
        if (isPendingTaskQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingTaskLists());
        }
        return Optional.empty();
    }



}
