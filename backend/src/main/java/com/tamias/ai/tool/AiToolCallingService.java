package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;
import java.text.Normalizer;
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
            return Optional.of(AiToolAnswer.of(
                    """
                    En esta versión puedo ayudarte a consultar y resumir información, pero no puedo crear, editar, eliminar registros ni enviar notificaciones automáticamente.

                    Puedo indicarte qué datos encontré para que tú hagas el cambio desde el módulo correspondiente.
                    """.trim(),
                    "assistant.readOnlyGuard",
                    "Read-only guard",
                    "Write action rejected by the read-only AI tool layer.",
                    List.of()
            ));
        }

        if (isCapabilityQuestion(normalized)) {
            return Optional.of(readOnlyToolService.capabilities());
        }
        if (isCurrentUserProfileQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentUserProfile(question));
        }
        if (isOrganizationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentOrganizationSummary());
        }
        if (isOperationalSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.operationalSummary());
        }
        if (isReservationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.upcomingReservations());
        }
        if (isOverdueScheduledMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.overdueScheduledMaintenance());
        }
        if (isMaintenanceLastPerformedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPerformedMaintenance(question));
        }
        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }
        if (isPendingTaskQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingTaskLists());
        }
        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }
        if (isDocumentMetadataQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentMetadata(question));
        }
        if (isPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.searchProperties(question));
        }

        return Optional.empty();
    }

    private boolean isCapabilityQuestion(String q) {
        return containsAny(q,
                "que puedes hacer",
                "que sabes hacer",
                "como me puedes ayudar",
                "como puedes ayudar",
                "que tipo de asistente eres",
                "que eres",
                "cuales son tus capacidades",
                "capacidades",
                "que puedo preguntarte",
                "ayuda",
                "help"
        );
    }

    private boolean isCurrentUserProfileQuestion(String q) {
        return containsAny(q,
                "como me llamo",
                "cual es mi nombre",
                "mi nombre",
                "cual es mi correo",
                "mi correo",
                "mi email",
                "usuario actual",
                "que usuario estoy usando",
                "mi usuario",
                "cual es mi rol",
                "mi rol",
                "mi telefono",
                "numero de telefono",
                "recuerdame mi telefono"
        );
    }

    private boolean isOrganizationQuestion(String q) {
        return containsAny(q,
                "organizacion",
                "a que organizacion pertenezco",
                "mi organizacion"
        );
    }

    private boolean isOperationalSummaryQuestion(String q) {
        return containsAny(q,
                "resumen operativo",
                "estado general",
                "que necesita atencion",
                "pendiente antes del fin de semana",
                "estado de mis propiedades",
                "resumen de hoy"
        );
    }

    private boolean isReservationQuestion(String q) {
        return containsAny(q, "reserva", "reservacion", "check in", "check-in", "entrada", "llega", "llegan", "huesped")
                && containsAny(q, "semana", "proxima", "proximo", "hoy", "manana", "estos dias", "llega", "llegan", "check");
    }

    private boolean isMaintenanceLastPerformedQuestion(String q) {
        return containsAny(q, "mantenimiento", "filtro", "bomba", "cisterna", "pozo", "canalon", "pintura", "ducha", "porton")
                && containsAny(q, "ultimo", "ultima", "cuando", "hace cuanto");
    }

    private boolean isOverdueScheduledMaintenanceQuestion(String q) {
        return containsAny(q, "mantenimiento", "mantenimientos")
                && containsAny(q, "vencido", "vencidos", "atrasado", "atrasados");
    }

    private boolean isLastPurchaseQuestion(String q) {
        return containsAny(q, "compra", "compras", "compre", "comprado", "compraste")
                && containsAny(q, "ultimo", "ultima", "cuando", "hace cuanto");
    }

    private boolean isPendingTaskQuestion(String q) {
        return containsAny(q, "tarea", "tareas", "checklist")
                && containsAny(q, "pendiente", "pendientes", "abierta", "abiertas", "progreso", "vencida", "vencidas");
    }

    private boolean isDocumentMetadataQuestion(String q) {
        return containsAny(q, "documento", "documentos", "manual", "manuales", "reglas", "plano", "planos")
                && containsAny(q, "tengo", "procesado", "procesados", "estado", "tipo", "subido", "subidos", "cargado", "cargados");
    }

    private boolean isRagHealthQuestion(String q) {
        return containsAny(q,
                "vector store",
                "vector_store",
                "vector store id",
                "vector_store_id",
                "chunks",
                "indexacion",
                "indexados",
                "listos para ia",
                "documentos listos"
        );
    }

    private boolean isPropertyQuestion(String q) {
        return containsAny(q, "propiedad", "propiedades")
                && containsAny(q, "tengo", "activas", "registradas", "buscar", "busca", "resumen", "lista", "listar");
    }

    private boolean isUnsupportedWriteAction(String q) {
        return startsWithAny(q,
                "crea ",
                "crear ",
                "agrega ",
                "agregar ",
                "actualiza ",
                "actualizar ",
                "edita ",
                "editar ",
                "elimina ",
                "eliminar ",
                "borra ",
                "borrar ",
                "envia ",
                "mandale ",
                "notifica ",
                "notificar "
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

    private boolean startsWithAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.startsWith(normalize(candidate))) {
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
                .replaceAll("\\s+", " ")
                .trim();
    }
}
