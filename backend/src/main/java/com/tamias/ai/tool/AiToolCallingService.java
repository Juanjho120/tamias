package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiToolCallingService {

    private final AiReadOnlyToolService readOnlyToolService;

    public AiToolCallingService(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    public Optional<AiToolAnswer> tryHandle(AiChatRequest request) {
        String question = request.question();
        String normalized = normalize(question);

        if (isUnsupportedWriteAction(normalized)) {
            return Optional.of(readOnlyGuard());
        }

        if (isCapabilitiesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.capabilities());
        }

        if (isCurrentUserProfileQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentUserProfile(question));
        }

        if (isOrganizationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentOrganizationSummary());
        }

        Optional<AiToolAnswer> assistantAnswer = tryHandleAssistantLevelQuestion(question, normalized);
        if (assistantAnswer.isPresent()) {
            return assistantAnswer;
        }

        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }

        if (isDocumentMetadataQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentMetadata(question));
        }

        if (isOperationalSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.operationalSummary());
        }

        if (isUpcomingReservationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.upcomingReservations());
        }

        if (isLastMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPerformedMaintenance(question));
        }

        if (isOverdueScheduledMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.overdueScheduledMaintenance());
        }

        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }

        if (isPendingTaskQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingTaskLists());
        }

        if (isPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.searchProperties(question));
        }

        return Optional.empty();
    }

    private Optional<AiToolAnswer> tryHandleAssistantLevelQuestion(String question, String normalized) {
        if (isPreparationQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.operationalPreparation",
                    "Operational preparation assistant",
                    "Upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Revisé tus próximas reservaciones, mantenimientos vencidos y tareas pendientes para darte una visión rápida de preparación.",
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
                    readOnlyToolService.searchProperties(question),
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.lastPerformedMaintenance(question),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.propertyOperations",
                    "Property operations assistant",
                    "Properties, upcoming reservations, last maintenance and pending tasks were consulted together.",
                    "Conecté la información de propiedades con reservaciones, mantenimiento reciente y tareas pendientes para darte contexto operativo.",
                    answers
            ));
        }

        return Optional.empty();
    }

    private AiToolAnswer combine(
            String toolName,
            String label,
            String summary,
            String intro,
            List<AiToolAnswer> answers
    ) {
        StringBuilder builder = new StringBuilder(intro).append("\n\n");
        List<AiToolEvidenceResponse> evidence = new ArrayList<>();

        for (AiToolAnswer answer : answers) {
            evidence.addAll(answer.evidence());
            builder.append("### ").append(resolveSectionTitle(answer)).append("\n")
                    .append(answer.answer())
                    .append("\n\n");
        }

        builder.append("No hice cambios en tus datos; esta respuesta solo consulta información existente en TAMIAS.");

        evidence.add(new AiToolEvidenceResponse(toolName, label, summary, List.of()));
        return new AiToolAnswer(builder.toString().trim(), true, evidence);
    }

    private String resolveSectionTitle(AiToolAnswer answer) {
        if (answer.evidence().isEmpty()) {
            return "Resultado consultado";
        }

        String toolName = answer.evidence().get(0).toolName();
        return switch (toolName) {
            case "dashboard.operationalSummary" -> "Resumen operativo";
            case "reservation.upcoming" -> "Reservaciones próximas";
            case "scheduledMaintenance.overdue" -> "Mantenimientos programados vencidos";
            case "taskList.pending" -> "Tareas pendientes";
            case "document.searchMetadata" -> "Documentos cargados";
            case "rag.documentIndexStatus" -> "Índice RAG";
            case "property.search" -> "Propiedades";
            case "maintenance.lastPerformed" -> "Último mantenimiento";
            case "purchaseItem.lastPurchased" -> "Última compra";
            default -> answer.evidence().get(0).label();
        };
    }

    private AiToolAnswer readOnlyGuard() {
        return AiToolAnswer.of(
                "Puedo ayudarte a revisar información que ya existe en TAMIAS, pero por seguridad todavía no creo, edito, elimino ni envío datos desde el asistente. "
                        + "Dime qué quieres revisar y te ayudo a encontrarlo para que tú hagas el cambio desde el módulo correspondiente.",
                "assistant.readOnlyGuard",
                "Read-only guard",
                "The user asked for an action that would modify data. The assistant refused autonomous writes.",
                List.of()
        );
    }

    private boolean isCapabilitiesQuestion(String value) {
        return containsAny(value,
                "que puedes hacer",
                "que sabes hacer",
                "como me ayudas",
                "en que me ayudas",
                "que tipo de asistente eres",
                "capacidades",
                "funciones",
                "herramientas",
                "que puedes consultar"
        );
    }

    private boolean isCurrentUserProfileQuestion(String value) {
        return containsAny(value,
                "como me llamo",
                "cual es mi nombre",
                "mi nombre",
                "cual es mi correo",
                "mi correo",
                "mi email",
                "que usuario estoy usando",
                "usuario estoy usando",
                "mi usuario",
                "cual es mi rol",
                "mi rol",
                "mi perfil",
                "perfil actual",
                "numero de telefono",
                "mi telefono",
                "mi celular",
                "recuerdame mi numero"
        );
    }

    private boolean isOrganizationQuestion(String value) {
        return containsAny(value,
                "mi organizacion",
                "organizacion actual",
                "empresa actual",
                "resumen de organizacion",
                "resumen de la organizacion"
        );
    }

    private boolean isPropertyQuestion(String value) {
        return containsAny(value,
                "propiedad",
                "propiedades",
                "alojamiento",
                "alojamientos",
                "bungalow",
                "bungalows",
                "casa",
                "casas",
                "cabin",
                "cabins",
                "cabana",
                "cabanas"
        );
    }

    private boolean isOperationalSummaryQuestion(String value) {
        return containsAny(value,
                "resumen operativo",
                "dashboard",
                "panel operativo",
                "estado operativo",
                "resumen del sistema",
                "metricas operativas"
        );
    }

    private boolean isUpcomingReservationQuestion(String value) {
        return containsAny(value,
                "reservaciones proximas",
                "reservas proximas",
                "reservaciones activas",
                "reservas activas",
                "proximas reservaciones",
                "proximas reservas",
                "check in proximos",
                "check-in proximos",
                "entradas proximas"
        ) || (containsAny(value, "reservacion", "reservaciones", "reserva", "reservas")
                && containsAny(value, "proxima", "proximas", "siguiente", "siguientes", "activas", "check in", "check-in"));
    }

    private boolean isLastMaintenanceQuestion(String value) {
        return containsAny(value,
                "ultimo mantenimiento",
                "ultima reparacion",
                "mantenimiento mas reciente",
                "ultimo trabajo realizado",
                "mantenimiento realizado",
                "mantenimiento completado"
        ) || (containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones")
                && containsAny(value, "ultimo", "ultima", "reciente", "realizado", "completado"));
    }

    private boolean isOverdueScheduledMaintenanceQuestion(String value) {
        return containsAny(value,
                "mantenimientos programados vencidos",
                "mantenimiento programado vencido",
                "programados vencidos",
                "vencidos",
                "atrasados",
                "caducados"
        ) && containsAny(value, "mantenimiento", "mantenimientos", "programado", "programados");
    }

    private boolean isLastPurchaseQuestion(String value) {
        return containsAny(value,
                "ultima compra",
                "ultimo item comprado",
                "ultimo producto comprado",
                "compre por ultima vez",
                "compraste por ultima vez",
                "cuando compre",
                "cuando se compro",
                "comprado por ultima vez"
        ) || (containsAny(value, "compra", "compras", "compre", "comprado", "compraste")
                && containsAny(value, "ultima", "ultimo", "vez", "cuando", "reciente"));
    }

    private boolean isPendingTaskQuestion(String value) {
        return containsAny(value,
                "tareas pendientes",
                "listas de tareas pendientes",
                "task lists pendientes",
                "pendientes tengo",
                "tareas abiertas",
                "listas abiertas",
                "tareas en progreso",
                "cosas pendientes",
                "algo pendiente"
        ) || (containsAny(value, "tarea", "tareas", "task", "tasks", "pendiente", "pendientes")
                && containsAny(value, "pendiente", "pendientes", "abierta", "abiertas", "progreso", "hacer"));
    }

    private boolean isDocumentMetadataQuestion(String value) {
        return containsAny(value,
                "documentos cargados",
                "documentos subidos",
                "documentos tengo",
                "documentos registrados",
                "documentos procesados",
                "que documentos",
                "mis documentos",
                "document metadata"
        );
    }

    private boolean isRagHealthQuestion(String value) {
        return containsAny(value,
                "indice rag",
                "index rag",
                "rag de mis documentos",
                "estado rag",
                "estado del rag",
                "indexacion ia",
                "indexacion de documentos",
                "estado de indexacion",
                "documentos indexados",
                "chunks indexados",
                "vector store",
                "vector_store",
                "chroma"
        );
    }

    private boolean isPreparationQuestion(String value) {
        return containsAny(value,
                "preparar la casa",
                "preparar propiedad",
                "preparar alojamiento",
                "antes de la proxima reserva",
                "antes de la proxima reservacion",
                "antes del proximo check in",
                "antes del check in",
                "para la proxima reserva",
                "para la proxima reservacion"
        ) || (containsAny(value, "preparar", "pendiente", "pendientes", "hacer", "falta")
                && containsAny(value, "reserva", "reservacion", "check in", "huesped", "alojamiento", "casa", "propiedad"));
    }

    private boolean isOperationalPlanningQuestion(String value) {
        return containsAny(value,
                "que debo atender",
                "que debo revisar",
                "que tengo pendiente hoy",
                "prioridades operativas",
                "plan operativo",
                "que necesita atencion",
                "como va la operacion",
                "estado general de la operacion"
        );
    }

    private boolean isDocumentInventoryQuestion(String value) {
        return isDocumentMetadataQuestion(value) && isRagHealthQuestion(value);
    }

    private boolean isPropertyOperationsQuestion(String value) {
        return isPropertyQuestion(value)
                && containsAny(value, "operacion", "operativo", "reservacion", "reserva", "mantenimiento", "tarea", "pendiente", "estado");
    }

    private boolean isUnsupportedWriteAction(String value) {
        return startsWithAny(value,
                "crea ",
                "crear ",
                "agrega ",
                "agregar ",
                "anade ",
                "anadir ",
                "registra ",
                "registrar ",
                "actualiza ",
                "actualizar ",
                "edita ",
                "editar ",
                "modifica ",
                "modificar ",
                "elimina ",
                "eliminar ",
                "borra ",
                "borrar ",
                "cancela ",
                "cancelar ",
                "envia ",
                "enviar ",
                "manda ",
                "mandar ",
                "programa ",
                "programar "
        );
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(normalize(prefix))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized
                .toLowerCase()
                .replace("¿", " ")
                .replace("?", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replace(":", " ")
                .replace(";", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
