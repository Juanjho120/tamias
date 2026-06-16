package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(80)
public class PurchaseAnalyticsToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public PurchaseAnalyticsToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandlePurchaseAnalyticsQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandlePurchaseAnalyticsQuestion(String question, String normalized) {
        if (!isPurchaseAnalyticsQuestion(normalized)) {
            return Optional.empty();
        }
        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }
        if (isPurchaseItemCostTrendQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemCostTrend(question));
        }
        if (isPurchaseItemAverageUnitCostQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemAverageUnitCost(question));
        }
        if (isPurchaseItemPriceHistoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemPriceHistory(question));
        }
        if (isPurchaseItemMostPurchasedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemMostPurchased());
        }
        if (isPurchaseItemLeastPurchasedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemLeastPurchased());
        }
        if (isPurchaseItemQuantitySummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemQuantitySummary(question));
        }
        if (isPurchaseCostByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByProperty());
        }
        if (isPurchaseCostByCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByCategory());
        }
        if (isPurchaseCostByMonthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByMonth());
        }
        if (isPurchaseCostSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostSummary(question));
        }
        if (isPurchaseListPendingQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingPurchaseLists());
        }
        if (isPurchaseListCompletedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.completedPurchaseLists());
        }
        if (isPurchaseListByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseListsByProperty(question));
        }
        if (isPurchaseListRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentPurchaseLists());
        }
        if (isPurchaseItemListQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemSearch(question));
        }
        return Optional.of(readOnlyToolService.purchaseListSearch(question));
    }
}
