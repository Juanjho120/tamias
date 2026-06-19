package com.tamias.ai.planning;

import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.tool.AiToolAnswer;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiAnswerCompositionService {

    private final ChatClient chatClient;
    private final boolean composeToolAnswers;

    public AiAnswerCompositionService(
            ChatModel chatModel,
            @Value("${tamias.ai.planning.compose-tool-answers:true}") boolean composeToolAnswers
    ) {
        this.chatClient = ChatClient.create(chatModel);
        this.composeToolAnswers = composeToolAnswers;
    }

    public String composeToolAnswer(String question, AiToolAnswer toolAnswer, AiExecutionPlan plan) {
        if (!composeToolAnswers || toolAnswer == null || toolAnswer.answer() == null || toolAnswer.answer().isBlank()) {
            return toolAnswer != null ? toolAnswer.answer() : "";
        }
        if (shouldReturnBackendAnswerAsIs(toolAnswer)) {
            return toolAnswer.answer();
        }

        try {
            return chatClient.prompt()
                    .system(toolAnswerSystemPrompt())
                    .user(toolAnswerUserPrompt(question, toolAnswer, plan))
                    .call()
                    .content();
        } catch (Exception exception) {
            return toolAnswer.answer();
        }
    }

    private boolean shouldReturnBackendAnswerAsIs(AiToolAnswer toolAnswer) {
        String toolName = primaryToolName(toolAnswer);
        return toolName.equals("assistant.capabilities")
                || toolName.equals("assistant.readOnlyGuard")
                || toolName.startsWith("user.")
                || toolName.startsWith("organization.")
                || toolName.startsWith("role.")
                || toolName.startsWith("catalog.")
                || toolName.startsWith("property.")
                || toolName.startsWith("reservation.")
                || toolName.startsWith("guest.")
                || toolName.startsWith("scheduledMaintenance.")
                || toolName.startsWith("maintenance.")
                || toolName.startsWith("reservationSupply.")
                || toolName.startsWith("taskList.")
                || toolName.startsWith("taskItem.")
                || toolName.startsWith("purchase.")
                || toolName.startsWith("purchaseList.")
                || toolName.startsWith("purchaseItem.")
                || toolName.startsWith("inventory.")
                || toolName.startsWith("file.")
                || toolName.startsWith("files.")
                || toolName.startsWith("image.")
                || toolName.startsWith("document.")
                || toolName.startsWith("rag.")
                || toolName.startsWith("dashboard.")
                || toolName.startsWith("aiChat.")
                || toolName.startsWith("assistant.");
    }

    private String primaryToolName(AiToolAnswer toolAnswer) {
        if (toolAnswer == null || toolAnswer.evidence() == null || toolAnswer.evidence().isEmpty()) {
            return "";
        }
        String toolName = toolAnswer.evidence().get(0).toolName();
        return toolName == null ? "" : toolName;
    }

    private String toolAnswerSystemPrompt() {
        return """
                Eres TAMIAS, un asistente para administrar alojamientos.

                Redacta una respuesta natural usando únicamente los datos estructurados proporcionados por el backend.

                Reglas estrictas:
                1. Responde en el mismo idioma de la pregunta del usuario.
                2. No inventes datos, nombres, permisos, costos, fechas, URLs, IDs ni recomendaciones.
                3. Conserva el significado de la respuesta del backend.
                4. Si el backend dice que no muestra contraseñas, hashes, tokens o datos internos, conserva esa advertencia.
                5. Si la respuesta ya es corta y clara, puedes dejarla casi igual.
                6. No agregues markdown excesivo.
                """;
    }

    private String toolAnswerUserPrompt(String question, AiToolAnswer toolAnswer, AiExecutionPlan plan) {
        return """
                Pregunta del usuario:
                %s

                Plan de ejecución:
                %s

                Respuesta estructurada generada por el backend:
                %s

                Evidencia disponible:
                %s

                Redacta la respuesta final. Usa solo esa información.
                """.formatted(
                question,
                planSummary(plan),
                toolAnswer.answer(),
                evidenceSummary(toolAnswer.evidence())
        );
    }

    private String planSummary(AiExecutionPlan plan) {
        if (plan == null) {
            return "No plan metadata.";
        }
        return "decision=" + plan.safeDecision()
                + "; confidence=" + plan.confidence()
                + "; reason=" + plan.reason();
    }

    private String evidenceSummary(List<AiToolEvidenceResponse> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "No evidence metadata.";
        }
        StringBuilder builder = new StringBuilder();
        for (AiToolEvidenceResponse item : evidence) {
            builder.append("- tool=").append(item.toolName())
                    .append("; label=").append(item.label())
                    .append("; summary=").append(item.summary())
                    .append("; itemCount=").append(item.items() == null ? 0 : item.items().size())
                    .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }
}
