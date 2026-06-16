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
@Order(100)
public class InventoryToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public InventoryToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleInventoryQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleInventoryQuestion(String question, String normalized) {
        if (!isInventoryQuestion(normalized) && !isInventoryWhereUsedQuestion(normalized)) {
            return Optional.empty();
        }
        if (isInventoryWhereUsedQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.inventoryMaintenanceUsage(question),
                    readOnlyToolService.inventoryReservationUsage(question),
                    readOnlyToolService.inventoryPurchaseUsage(question)
            );
            return Optional.of(combine(
                    "inventory.whereUsed",
                    "Inventory where-used lookup",
                    "Maintenance, reservation and purchase usage were consulted for the requested item.",
                    "Busqué dónde aparece ese item dentro de mantenimientos, reservaciones y compras.",
                    answers
            ));
        }
        if (isInventoryUnusedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryUnusedItems());
        }
        if (isInventoryReservationUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryReservationUsage(""));
        }
        if (isInventoryMaintenanceUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(""));
        }
        if (isInventoryPurchaseUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryPurchaseUsage(""));
        }
        if (isInventoryReservationUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryReservationUsage(question));
        }
        if (isInventoryMaintenanceUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        if (isInventoryPurchaseUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryPurchaseUsage(question));
        }
        if (isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryFrequentlyUsed());
        }
        return Optional.of(readOnlyToolService.inventorySearch(question));
    }
}
