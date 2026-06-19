package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.InventoryToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryReadOnlyToolService {

    private final InventoryToolRepository repository;

    public InventoryReadOnlyToolService(InventoryToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer inventorySearch(String userQuestion) {
        return repository.inventorySearch(userQuestion);
    }

    public AiToolAnswer inventoryItemsByBrand(String userQuestion) {
        return repository.inventoryItemsByBrand(userQuestion);
    }

    public AiToolAnswer inventoryFrequentlyUsed() {
        return repository.inventoryFrequentlyUsed();
    }

    public AiToolAnswer inventoryUnusedItems() {
        return repository.inventoryUnusedItems();
    }

    public AiToolAnswer inventoryReservationUsage(String userQuestion) {
        return repository.inventoryReservationUsage(userQuestion);
    }

    public AiToolAnswer inventoryPurchaseUsage(String userQuestion) {
        return repository.inventoryPurchaseUsage(userQuestion);
    }

    public AiToolAnswer inventoryMaintenanceUsage(String userQuestion) {
        return repository.inventoryMaintenanceUsage(userQuestion);
    }
}
