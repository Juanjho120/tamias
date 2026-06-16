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
@Order(60)
public class PriorityMaintenanceAnalyticsToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public PriorityMaintenanceAnalyticsToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandlePriorityMaintenanceAnalyticsQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandlePriorityMaintenanceAnalyticsQuestion(String question, String normalized) {
        if (!isMaintenanceAnalyticsQuestion(normalized)) {
            return Optional.empty();
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
        if (isMaintenanceImageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceImagesSummary(containsAny(normalized, "sin imagen", "sin imagenes", "no tienen imagen", "no tiene imagen", "sin evidencia", "no tienen evidencia", "no tiene evidencia")));
        }
        if (isMaintenanceItemUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        return Optional.empty();
    }
}
