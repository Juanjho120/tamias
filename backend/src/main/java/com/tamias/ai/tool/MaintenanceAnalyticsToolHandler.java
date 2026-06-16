package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(150)
public class MaintenanceAnalyticsToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public MaintenanceAnalyticsToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleMaintenanceAnalyticsQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleMaintenanceAnalyticsQuestion(String question, String normalized) {
        if (!isMaintenanceAnalyticsQuestion(normalized)) {
            return Optional.empty();
        }
        if (isMaintenanceImageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceImagesSummary(containsAny(normalized, "sin imagen", "sin imagenes", "no tienen imagen", "no tiene imagen", "sin evidencia", "no tienen evidencia", "no tiene evidencia")));
        }
        if (isMaintenanceCostByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByProperty());
        }
        if (isMaintenanceCostByCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByCategory());
        }
        if (isMaintenanceCostByMonthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByMonth());
        }
        if (isMaintenanceCostQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostSummary(question));
        }
        if (isMaintenanceStatusQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByStatus(question));
        }
        if (isMaintenancePropertyFilterQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByProperty(question));
        }
        if (isMaintenanceCategoryOrTypeFilterQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByCategoryOrType(question));
        }
        if (isMaintenanceItemUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        if (isMaintenanceRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentMaintenance());
        }
        return Optional.of(readOnlyToolService.maintenanceSearch(question));
    }
}
