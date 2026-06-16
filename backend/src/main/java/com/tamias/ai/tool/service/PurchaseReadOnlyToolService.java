package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchaseReadOnlyToolService extends AiReadOnlyToolSupport {

    public PurchaseReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer lastPurchasedItem(String userQuestion) {
        return super.lastPurchasedItem(userQuestion);
    }

    public AiToolAnswer purchaseListSearch(String userQuestion) {
        return super.purchaseListSearch(userQuestion);
    }

    public AiToolAnswer purchaseListsByProperty(String userQuestion) {
        return super.purchaseListsByProperty(userQuestion);
    }

    public AiToolAnswer recentPurchaseLists() {
        return super.recentPurchaseLists();
    }

    public AiToolAnswer pendingPurchaseLists() {
        return super.pendingPurchaseLists();
    }

    public AiToolAnswer completedPurchaseLists() {
        return super.completedPurchaseLists();
    }

    public AiToolAnswer purchaseCostSummary(String userQuestion) {
        return super.purchaseCostSummary(userQuestion);
    }

    public AiToolAnswer purchaseCostByProperty() {
        return super.purchaseCostByProperty();
    }

    public AiToolAnswer purchaseCostByCategory() {
        return super.purchaseCostByCategory();
    }

    public AiToolAnswer purchaseCostByMonth() {
        return super.purchaseCostByMonth();
    }

    public AiToolAnswer purchaseItemSearch(String userQuestion) {
        return super.purchaseItemSearch(userQuestion);
    }

    public AiToolAnswer purchaseItemsByPurchaseList(String userQuestion) {
        return super.purchaseItemsByPurchaseList(userQuestion);
    }

    public AiToolAnswer purchaseItemsByInventoryItem(String userQuestion) {
        return super.purchaseItemsByInventoryItem(userQuestion);
    }

    public AiToolAnswer purchaseItemPriceHistory(String userQuestion) {
        return super.purchaseItemPriceHistory(userQuestion);
    }

    public AiToolAnswer purchaseItemAverageUnitCost(String userQuestion) {
        return super.purchaseItemAverageUnitCost(userQuestion);
    }

    public AiToolAnswer purchaseItemQuantitySummary(String userQuestion) {
        return super.purchaseItemQuantitySummary(userQuestion);
    }

    public AiToolAnswer purchaseItemMostPurchased() {
        return super.purchaseItemMostPurchased();
    }

    public AiToolAnswer purchaseItemLeastPurchased() {
        return super.purchaseItemLeastPurchased();
    }

    public AiToolAnswer purchaseItemCostTrend(String userQuestion) {
        return super.purchaseItemCostTrend(userQuestion);
    }

}
