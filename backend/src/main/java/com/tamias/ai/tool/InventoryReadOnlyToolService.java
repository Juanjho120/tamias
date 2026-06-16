package com.tamias.ai.tool;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryReadOnlyToolService extends AiReadOnlyToolSupport {

    public InventoryReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer inventorySearch(String userQuestion) {
        return super.inventorySearch(userQuestion);
    }

    public AiToolAnswer inventoryFrequentlyUsed() {
        return super.inventoryFrequentlyUsed();
    }

    public AiToolAnswer inventoryUnusedItems() {
        return super.inventoryUnusedItems();
    }

    public AiToolAnswer inventoryReservationUsage(String userQuestion) {
        return super.inventoryReservationUsage(userQuestion);
    }

    public AiToolAnswer inventoryPurchaseUsage(String userQuestion) {
        return super.inventoryPurchaseUsage(userQuestion);
    }

    public AiToolAnswer inventoryMaintenanceUsage(String userQuestion) {
        return super.inventoryMaintenanceUsage(userQuestion);
    }

}
