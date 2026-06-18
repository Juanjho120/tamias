package com.tamias.ai.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamias.ai.tool.AiToolResult;
import com.tamias.ai.tool.AiToolResultStatus;
import com.tamias.ai.tool.support.AiToolTextNormalizer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiPlanningService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public AiPlanningService(
            ChatModel chatModel,
            ObjectMapper objectMapper,
            @Value("${tamias.ai.planning.enabled:true}") boolean enabled
    ) {
        this.chatClient = ChatClient.create(chatModel);
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public AiExecutionPlan plan(String question, AiToolResult toolResult) {
        if (!enabled) {
            return heuristicPlan(question, toolResult, "LLM planning disabled by configuration.");
        }

        if (toolResult != null
                && (toolResult.status() == AiToolResultStatus.GUARDRAIL || toolResult.status() == AiToolResultStatus.DENIED)) {
            return AiExecutionPlan.toolFirst("Security-sensitive deterministic tool result must be respected before LLM planning.");
        }

        String normalized = AiToolTextNormalizer.normalizeForRouting(question);
        if (looksExplicitlyToolAndRag(normalized)) {
            return new AiExecutionPlan(AiPlanDecisionType.TOOL_AND_RAG, "Question explicitly asks for both system records and documents.", 1.0, false);
        }
        if (looksDocumentCentric(normalized) && toolResult != null && toolResult.status() == AiToolResultStatus.HIT && primaryToolName(toolResult).startsWith("document.")) {
            return AiExecutionPlan.ragOnly("Question asks for document content; metadata tool hit should not replace RAG.");
        }
        if (toolResult != null
                && toolResult.status() == AiToolResultStatus.HIT
                && isStructuredToolHitThatShouldBeRespected(toolResult)
                && !looksDocumentCentric(normalized)) {
            return AiExecutionPlan.toolFirst("Deterministic structured system tool produced a relevant hit.");
        }

        try {
            String raw = chatClient.prompt()
                    .system(systemPrompt())
                    .user(userPrompt(question, toolResult))
                    .call()
                    .content();
            return parsePlan(raw, question, toolResult);
        } catch (Exception exception) {
            return heuristicPlan(question, toolResult, "LLM planning failed; using deterministic fallback: " + exception.getClass().getSimpleName());
        }
    }

    private AiExecutionPlan parsePlan(String raw, String question, AiToolResult toolResult) {
        String json = extractJsonObject(raw);
        if (json.isBlank()) {
            return heuristicPlan(question, toolResult, "LLM planner did not return a JSON object.");
        }

        try {
            AiPlanResponse response = objectMapper.readValue(json, AiPlanResponse.class);
            AiPlanDecisionType decision = parseDecision(response.decision());
            if (decision == null) {
                return heuristicPlan(question, toolResult, "LLM planner returned an unknown decision.");
            }
            double confidence = response.confidence() == null ? 0.0 : clamp(response.confidence());
            return new AiExecutionPlan(decision, safeReason(response.reason()), confidence, true);
        } catch (Exception exception) {
            return heuristicPlan(question, toolResult, "LLM planner JSON could not be parsed: " + exception.getClass().getSimpleName());
        }
    }

    private AiExecutionPlan heuristicPlan(String question, AiToolResult toolResult, String reason) {
        String normalized = AiToolTextNormalizer.normalizeForRouting(question);

        if (looksLikeWriteAction(normalized)) {
            return AiExecutionPlan.denyWrite(reason + " Write-like request detected.");
        }

        if (looksExplicitlyToolAndRag(normalized)) {
            return new AiExecutionPlan(AiPlanDecisionType.TOOL_AND_RAG, reason + " Explicit system-and-document question detected.", 1.0, false);
        }

        if (looksDocumentCentric(normalized)) {
            return AiExecutionPlan.ragOnly(reason + " Document-centric question detected.");
        }

        if (toolResult != null && toolResult.status() == AiToolResultStatus.HIT) {
            return AiExecutionPlan.toolFirst(reason + " Deterministic tool produced a hit.");
        }

        return AiExecutionPlan.defaultPlan(reason);
    }

    private boolean isStructuredToolHitThatShouldBeRespected(AiToolResult toolResult) {
        String toolName = primaryToolName(toolResult);
        if (toolName.isBlank()) {
            return false;
        }
        return toolName.startsWith("user.")
                || toolName.startsWith("role.")
                || toolName.startsWith("organization.")
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
                || toolName.startsWith("document.")
                || toolName.startsWith("rag.")
                || toolName.startsWith("dashboard.")
                || toolName.startsWith("aiChat.")
                || toolName.startsWith("assistant.");
    }

    private String primaryToolName(AiToolResult toolResult) {
        return toolResult == null || toolResult.answerOptional().isEmpty() || toolResult.answer().evidence().isEmpty()
                ? ""
                : String.valueOf(toolResult.answer().evidence().get(0).toolName());
    }

    private String systemPrompt() {
        return """
                You are the planning layer for TAMIAS, a lodging management assistant.

                Decide which information path should be used. Return ONLY a JSON object with this exact shape:
                {"decision":"TOOL_FIRST|RAG_FIRST|TOOL_ONLY|RAG_ONLY|TOOL_AND_RAG|CLARIFY|DENY_WRITE","reason":"short reason","confidence":0.0}

                Definitions:
                - TOOL_FIRST: structured system data should be tried first; RAG can be used if the tool is empty and fallback is allowed.
                - RAG_FIRST: document content should be searched first; structured tools can be used only if documents are empty or context needs operational data.
                - TOOL_ONLY: the question is strictly about structured operational data such as users, roles, counts, dashboards, reservations, tasks, purchases or maintenance records.
                - RAG_ONLY: the question is about PDF/document content, rules, manuals, plans, policies, instructions, or what a document says.
                - TOOL_AND_RAG: the final answer likely needs both structured system data and document content.
                - CLARIFY: the request is too ambiguous to route safely.
                - DENY_WRITE: the user asks to create, update, delete, send, approve, reject, or perform another write action.

                Security rules:
                - Never decide to bypass backend permissions.
                - Backend owns user_id and organization_id.
                - Do not invent tool names or SQL.
                - If the user asks what a PDF/manual/rule/document says, prefer RAG_ONLY or RAG_FIRST.
                - If the user asks for counts, lists, statuses, dashboards, users or roles, prefer TOOL_ONLY or TOOL_FIRST.
                """;
    }

    private String userPrompt(String question, AiToolResult toolResult) {
        return """
                User question:
                %s

                Deterministic router pre-check:
                %s

                Return only JSON. No markdown. No explanation outside the JSON.
                """.formatted(question, toolResultSummary(toolResult));
    }

    private String toolResultSummary(AiToolResult toolResult) {
        if (toolResult == null) {
            return "No deterministic tool pre-check result.";
        }
        String toolName = toolResult.answerOptional()
                .flatMap(answer -> answer.evidence().stream().findFirst())
                .map(evidence -> evidence.toolName() == null ? "" : evidence.toolName())
                .orElse("");
        return "status=" + toolResult.status()
                + "; tool=" + toolName
                + "; allowRagFallback=" + toolResult.allowRagFallback();
    }

    private AiPlanDecisionType parseDecision(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = AiToolTextNormalizer.normalizeForRouting(value);
        String compact = toEnumToken(normalized);
        for (AiPlanDecisionType candidate : AiPlanDecisionType.values()) {
            if (candidate.name().equals(compact)) {
                return candidate;
            }
        }
        return null;
    }

    private String toEnumToken(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean previousUnderscore = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current >= 'a' && current <= 'z') {
                builder.append((char) (current - 32));
                previousUnderscore = false;
            } else if (current >= 'A' && current <= 'Z') {
                builder.append(current);
                previousUnderscore = false;
            } else if (current >= '0' && current <= '9') {
                builder.append(current);
                previousUnderscore = false;
            } else if (!previousUnderscore && builder.length() > 0) {
                builder.append('_');
                previousUnderscore = true;
            }
        }
        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == '_') {
            builder.deleteCharAt(length - 1);
        }
        return builder.toString();
    }

    private String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    return raw.substring(start, i + 1);
                }
            }
        }
        return "";
    }

    private boolean looksDocumentCentric(String normalized) {
        return AiToolTextNormalizer.containsAnyForRouting(
                normalized,
                "que dice", "qué dice", "que menciona", "qué menciona", "menciona", "habla de", "contenido",
                "segun el documento", "según el documento", "segun el pdf", "según el pdf", "en el pdf",
                "en el documento", "texto del documento", "regla del documento", "reglas del documento",
                "manual dice", "plano dice", "pdf dice", "documento dice", "que reglas hay", "qué reglas hay",
                "reglas hay", "reglas aplican", "que reglas aplican", "qué reglas aplican", "aplican a", "aplica a"
        );
    }

    private boolean looksExplicitlyToolAndRag(String normalized) {
        return AiToolTextNormalizer.containsAnyForRouting(
                normalized,
                "documentos y datos del sistema", "datos del sistema y documentos", "registros y documentos", "documentos y registros",
                "mis documentos y datos", "mis registros y documentos"
        );
    }

    private boolean looksLikeWriteAction(String normalized) {
        return AiToolTextNormalizer.containsAnyForRouting(
                normalized,
                "crea", "crear", "agrega", "agregar", "actualiza", "actualizar", "edita", "editar",
                "elimina", "eliminar", "borra", "borrar", "cambia", "cambiar", "envia", "enviar",
                "aprueba", "aprobar", "rechaza", "rechazar", "marca como", "completa", "completar"
        );
    }

    private String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "LLM planner selected an execution path.";
        }
        String normalized = reason.trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240).trim();
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
