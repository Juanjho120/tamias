package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(140)
public class CatalogToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public CatalogToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleCatalogQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleCatalogQuestion(String question, String normalized) {
        if (!isCatalogQuestion(normalized)) {
            return Optional.empty();
        }
        if (isMaintenanceCatalogOverviewQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCatalogOverview());
        }
        if (isMaintenanceCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCategories());
        }
        if (isMaintenanceTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceTypes());
        }
        if (isReservationPlatformQuestion(normalized)) {
            return Optional.of(readOnlyToolService.reservationPlatforms());
        }
        if (isTaskCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.taskCategories());
        }
        if (isPurchaseCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCategories());
        }
        if (isInventoryItemTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryItemTypes());
        }
        return Optional.of(readOnlyToolService.catalogSearch(question));
    }
}
