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
@Order(120)
public class AssistantLevelToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public AssistantLevelToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleAssistantLevelQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleAssistantLevelQuestion(String question, String normalized) {
        if (isPreparationQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.scheduledMaintenanceByStatus("mantenimientos programados activos"),
                    readOnlyToolService.maintenanceByStatus("mantenimientos pendientes"),
                    readOnlyToolService.pendingTaskItems(),
                    readOnlyToolService.pendingPurchaseLists()
            );
            return Optional.of(combine(
                    "assistant.operationalPreparation",
                    "Operational preparation assistant",
                    "Active scheduled maintenance, pending maintenance records, pending task items and pending purchase lists were consulted together.",
                    "Antes de la próxima reservación, revisa estos pendientes operativos del sistema.",
                    answers
            ));
        }
        if (isOperationalPlanningQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.operationalSummary(),
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.operationalPlanning",
                    "Operational planning assistant",
                    "Operational summary, upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Te dejo un panorama operativo combinando dashboard, próximas reservaciones, mantenimientos vencidos y tareas pendientes.",
                    answers
            ));
        }
        if (isDocumentInventoryQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.documentMetadata(question),
                    readOnlyToolService.ragDocumentIndexStatus()
            );
            return Optional.of(combine(
                    "assistant.documentOverview",
                    "Document overview assistant",
                    "Document metadata and RAG index status were consulted together.",
                    "Revisé tus documentos cargados y el estado del índice RAG para separar archivos disponibles de contenido ya indexado para IA.",
                    answers
            ));
        }
        if (isPropertyOperationsQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.propertyOperationalOverview(),
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.propertyOperations",
                    "Property operations assistant",
                    "Properties, upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Conecté la información de propiedades con reservaciones, mantenimientos vencidos y tareas pendientes para darte contexto operativo.",
                    answers
            ));
        }
        return Optional.empty();
    }
}
