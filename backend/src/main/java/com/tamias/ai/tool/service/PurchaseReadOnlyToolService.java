package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.PurchaseToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PurchaseReadOnlyToolService {

    private final PurchaseToolRepository repository;

    public PurchaseReadOnlyToolService(PurchaseToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer lastPurchasedItem(String userQuestion) {
        return repository.lastPurchasedItem(userQuestion);
    }

    public AiToolAnswer purchaseListSearch(String userQuestion) {
        return repository.purchaseListSearch(userQuestion);
    }

    public AiToolAnswer purchaseListsByProperty(String userQuestion) {
        return repository.purchaseListsByProperty(userQuestion);
    }

    public AiToolAnswer recentPurchaseLists() {
        return repository.recentPurchaseLists();
    }

    public AiToolAnswer pendingPurchaseLists() {
        return repository.pendingPurchaseLists();
    }

    public AiToolAnswer completedPurchaseLists() {
        return repository.completedPurchaseLists();
    }

    public AiToolAnswer purchaseCostSummary(String userQuestion) {
        return repository.purchaseCostSummary(userQuestion);
    }

    public AiToolAnswer purchaseCostByProperty() {
        return repository.purchaseCostByProperty();
    }

    public AiToolAnswer purchaseCostByCategory() {
        return repository.purchaseCostByCategory();
    }

    public AiToolAnswer purchaseCostByMonth() {
        return repository.purchaseCostByMonth();
    }

    public AiToolAnswer purchaseItemSearch(String userQuestion) {
        return repository.purchaseItemSearch(userQuestion);
    }

    public AiToolAnswer purchaseItemsByPurchaseList(String userQuestion) {
        return repository.purchaseItemsByPurchaseList(userQuestion);
    }

    public AiToolAnswer purchaseItemsByInventoryItem(String userQuestion) {
        return repository.purchaseItemsByInventoryItem(userQuestion);
    }

    public AiToolAnswer purchaseItemPriceHistory(String userQuestion) {
        return repository.purchaseItemPriceHistory(userQuestion);
    }

    public AiToolAnswer purchaseItemAverageUnitCost(String userQuestion) {
        return repository.purchaseItemAverageUnitCost(userQuestion);
    }

    public AiToolAnswer purchaseItemQuantitySummary(String userQuestion) {
        return repository.purchaseItemQuantitySummary(userQuestion);
    }

    public AiToolAnswer purchaseItemMostPurchased() {
        return repository.purchaseItemMostPurchased();
    }

    public AiToolAnswer purchaseItemLeastPurchased() {
        return repository.purchaseItemLeastPurchased();
    }

    public AiToolAnswer purchaseItemCostTrend(String userQuestion) {
        return repository.purchaseItemCostTrend(userQuestion);
    }
}
