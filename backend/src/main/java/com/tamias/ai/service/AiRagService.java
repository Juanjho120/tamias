package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatMessageDebugResponse;
import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiChatResponse;
import com.tamias.ai.dto.AiSearchRequest;
import com.tamias.ai.dto.AiSearchResponse;
import com.tamias.ai.dto.AiSourceResponse;
import com.tamias.ai.dto.AiToolDebugTrace;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatSession;
import com.tamias.ai.enums.AiAnswerSource;
import com.tamias.ai.enums.AiChatMessageRole;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.AiToolCallingService;
import com.tamias.ai.tool.AiToolResult;
import com.tamias.ai.tool.AiToolResultStatus;
import com.tamias.ai.planning.AiAnswerCompositionService;
import com.tamias.ai.planning.AiExecutionPlan;
import com.tamias.ai.planning.AiPlanningService;
import com.tamias.security.service.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRagService {

    private static final int MAX_SOURCE_EXCERPT_LENGTH = 1200;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final CurrentUserService currentUserService;
    private final AiChatSessionService chatSessionService;
    private final AiChatDebugTraceService debugTraceService;
    private final AiToolCallingService toolCallingService;
    private final AiPlanningService planningService;
    private final AiAnswerCompositionService answerCompositionService;
    private final int defaultTopK;
    private final double defaultSimilarityThreshold;

    public AiRagService(
            VectorStore vectorStore,
            ChatModel chatModel,
            CurrentUserService currentUserService,
            AiChatSessionService chatSessionService,
            AiChatDebugTraceService debugTraceService,
            AiToolCallingService toolCallingService,
            AiPlanningService planningService,
            AiAnswerCompositionService answerCompositionService,
            @Value("${tamias.ai.default-top-k:10}") int defaultTopK,
            @Value("${tamias.ai.default-similarity-threshold:0.30}") double defaultSimilarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.create(chatModel);
        this.currentUserService = currentUserService;
        this.chatSessionService = chatSessionService;
        this.debugTraceService = debugTraceService;
        this.toolCallingService = toolCallingService;
        this.planningService = planningService;
        this.answerCompositionService = answerCompositionService;
        this.defaultTopK = defaultTopK;
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
    }

    public AiSearchResponse search(AiSearchRequest request) {
        List<Document> matches = searchSimilarDocuments(
                request.question(),
                request.propertyId(),
                request.topK(),
                request.similarityThreshold()
        );
        List<AiSourceResponse> sources = toSourceResponses(matches);
        return new AiSearchResponse(request.question(), sources.size(), sources);
    }

    public AiChatResponse chat(AiChatRequest request) {
        AiChatSession session = chatSessionService.getOrCreateSession(
                request.chatSessionId(),
                request.propertyId(),
                request.title(),
                request.question()
        );

        AiChatMessage userMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.USER,
                request.question()
        );

        AiToolResult toolResult = toolCallingService.tryHandleResult(request);
        if (toolResult.status() == AiToolResultStatus.GUARDRAIL || toolResult.status() == AiToolResultStatus.DENIED) {
            return persistToolAnswer(session, userMessage, request, toolResult, AiExecutionPlan.toolFirst("Security result handled deterministically."));
        }

        AiExecutionPlan plan = planningService.plan(request.question(), toolResult);
        if (plan.shouldDenyWrite()) {
            AiToolResult guardrailResult = toolResult.hasAnswer()
                    ? toolResult
                    : AiToolResult.guardrail(plannerReadOnlyGuardAnswer());
            return persistToolAnswer(session, userMessage, request, guardrailResult, plan);
        }
        if (plan.shouldAskClarification()) {
            return persistToolAnswer(session, userMessage, request, AiToolResult.hit(plannerClarificationAnswer()), plan);
        }

        UUID effectivePropertyId = request.propertyId() != null
                ? request.propertyId()
                : (session.getProperty() != null ? session.getProperty().getId() : null);

        if (plan.prefersRagFirst()) {
            AiChatResponse ragFirstResponse = tryRagFirst(session, userMessage, request, effectivePropertyId, toolResult, plan);
            if (ragFirstResponse != null) {
                return ragFirstResponse;
            }
        }

        if (toolResult.shouldRespondImmediately() && !plan.wantsBoth()) {
            return persistToolAnswer(session, userMessage, request, toolResult, plan);
        }

        if (plan.toolOnly() && !toolResult.shouldAttemptRagFallback()) {
            return persistNoInformation(session, userMessage, request, toolResult, List.of(), List.of(), plan);
        }

        List<Document> matches = searchSimilarDocuments(
                request.question(),
                effectivePropertyId,
                request.topK(),
                request.similarityThreshold()
        );
        List<AiSourceResponse> sources = toSourceResponses(matches);
        List<AiToolEvidenceResponse> toolEvidence = toolEvidenceForResponse(toolResult, plan);

        if (matches.isEmpty()) {
            if (toolResult.status() == AiToolResultStatus.HIT) {
                return persistToolAnswer(session, userMessage, request, toolResult, plan);
            }
            return persistNoInformation(session, userMessage, request, toolResult, sources, toolEvidence, plan);
        }

        String answer = buildRagAnswer(request.question(), matches, toolResultForRagContext(toolResult, plan), plan);

        AiChatMessage assistantMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.ASSISTANT,
                answer
        );
        AiChatMessageDebugResponse debug = persistDebug(
                assistantMessage,
                buildDebugTrace(
                        toolResult,
                        plan,
                        toolEvidence.isEmpty() ? AiAnswerSource.RAG : AiAnswerSource.TOOLS_AND_RAG,
                        true,
                        null,
                        null
                )
        );

        return new AiChatResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                request.question(),
                answer,
                true,
                sources.size(),
                sources,
                toolEvidence,
                debug
        );
    }


    private AiToolAnswer plannerReadOnlyGuardAnswer() {
        return AiToolAnswer.of(
                "No puedo crear, editar, eliminar, aprobar, rechazar, enviar notificaciones ni modificar datos desde el asistente IA en esta fase. Puedo ayudarte a consultar la información disponible en TAMIAS.",
                "assistant.llmReadOnlyGuard",
                "LLM read-only guard",
                "LLM planner detected a write-like action and the backend blocked it before execution.",
                List.of()
        );
    }

    private AiToolAnswer plannerClarificationAnswer() {
        return AiToolAnswer.of(
                "No tengo suficiente claridad para decidir si debo consultar datos del sistema, documentos/RAG o ambos. Puedes reformular la pregunta indicando si quieres buscar en registros de TAMIAS, en documentos cargados o en ambos.",
                "assistant.llmClarification",
                "LLM clarification",
                "LLM planner requested clarification before choosing a data path.",
                List.of()
        );
    }

    private AiChatResponse tryRagFirst(
            AiChatSession session,
            AiChatMessage userMessage,
            AiChatRequest request,
            UUID effectivePropertyId,
            AiToolResult toolResult,
            AiExecutionPlan plan
    ) {
        List<Document> matches = searchSimilarDocuments(
                request.question(),
                effectivePropertyId,
                request.topK(),
                request.similarityThreshold()
        );
        List<AiSourceResponse> sources = toSourceResponses(matches);
        List<AiToolEvidenceResponse> toolEvidence = toolEvidenceForResponse(toolResult, plan);

        if (!matches.isEmpty()) {
            String answer = buildRagAnswer(request.question(), matches, toolResultForRagContext(toolResult, plan), plan);
            AiChatMessage assistantMessage = chatSessionService.saveMessage(
                    session,
                    AiChatMessageRole.ASSISTANT,
                    answer
            );
            AiChatMessageDebugResponse debug = persistDebug(
                    assistantMessage,
                    buildDebugTrace(
                            toolResult,
                            plan,
                            toolEvidence.isEmpty() ? AiAnswerSource.RAG : AiAnswerSource.TOOLS_AND_RAG,
                            true,
                            null,
                            null
                    )
            );
            return new AiChatResponse(
                    session.getId(),
                    userMessage.getId(),
                    assistantMessage.getId(),
                    request.question(),
                    answer,
                    true,
                    sources.size(),
                    sources,
                    toolEvidence,
                    debug
            );
        }

        if (plan.ragOnly()) {
            return persistNoInformation(session, userMessage, request, AiToolResult.notApplicable(), sources, List.of(), plan);
        }

        if (toolResult.shouldRespondImmediately()) {
            return persistToolAnswer(session, userMessage, request, toolResult, plan);
        }

        if (toolResult.status() == AiToolResultStatus.EMPTY || toolResult.status() == AiToolResultStatus.ERROR) {
            return persistNoInformation(session, userMessage, request, toolResult, sources, toolEvidence, plan);
        }

        return null;
    }

    private String buildRagAnswer(String question, List<Document> matches, AiToolResult toolResult, AiExecutionPlan plan) {
        String systemPrompt = """
                Eres TAMIAS, un asistente para administración de propiedades, alojamientos, mantenimiento, reservaciones y documentos internos.

                Reglas estrictas:
                1. Responde en el mismo idioma de la pregunta del usuario.
                2. Usa únicamente el CONTEXTO proporcionado y los datos estructurados del backend cuando estén presentes.
                3. No inventes datos, reglas, fechas, costos, nombres ni recomendaciones que no aparezcan en el contexto.
                4. Si la respuesta no está en el contexto, dilo claramente.
                5. Cuando uses documentos, cita las fuentes usando el formato [S1], [S2], etc.
                6. Si usas datos estructurados del sistema, no los cites como [S]; solo intégralos naturalmente.
                7. Sé claro, práctico y natural; evita sonar como plantilla repetida.
                8. Si el contexto tiene reglas, tareas o recomendaciones, sepáralas en secciones simples.
                9. Si el usuario pide crear, editar, eliminar o notificar, indícale que esta versión del asistente es de consulta.
                """;

        String toolFallbackContext = buildToolFallbackContext(toolResult);
        String userPrompt = toolFallbackContext.isBlank()
                ? """
                        Pregunta del usuario:
                        %s

                        Plan de ejecución LLM:
                        %s

                        CONTEXTO DOCUMENTAL:
                        %s
                        """.formatted(question, planSummary(plan), buildContext(matches))
                : """
                        Pregunta del usuario:
                        %s

                        Plan de ejecución LLM:
                        %s

                        Datos estructurados del sistema consultados antes o junto con RAG:
                        %s

                        Usa el CONTEXTO DOCUMENTAL de abajo para responder. Si los datos estructurados no encontraron registros, no repitas ese vacío si el contexto documental sí responde la pregunta. Si tanto los datos estructurados como documentos son relevantes, combínalos sin inventar.

                        CONTEXTO DOCUMENTAL:
                        %s
                        """.formatted(question, planSummary(plan), toolFallbackContext, buildContext(matches));

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    private AiChatResponse persistToolAnswer(
            AiChatSession session,
            AiChatMessage userMessage,
            AiChatRequest request,
            AiToolResult result,
            AiExecutionPlan plan
    ) {
        AiToolAnswer answer = result.answer();
        String finalAnswer = result.status() == AiToolResultStatus.HIT
                ? answerCompositionService.composeToolAnswer(request.question(), answer, plan)
                : answer.answer();

        AiChatMessage assistantMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.ASSISTANT,
                finalAnswer
        );
        AiChatMessageDebugResponse debug = persistDebug(
                assistantMessage,
                buildDebugTrace(
                        result,
                        plan,
                        answerSourceForToolAnswer(result, answer, finalAnswer),
                        false,
                        result.shouldAttemptRagFallback() ? "Tool result allowed RAG fallback but final response used backend answer." : null,
                        null
                )
        );

        return new AiChatResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                request.question(),
                finalAnswer,
                answer.grounded(),
                0,
                List.of(),
                answer.evidence(),
                debug
        );
    }

    private AiChatResponse persistNoInformation(
            AiChatSession session,
            AiChatMessage userMessage,
            AiChatRequest request,
            AiToolResult toolResult,
            List<AiSourceResponse> sources,
            List<AiToolEvidenceResponse> toolEvidence,
            AiExecutionPlan plan
    ) {
        String fallbackAnswer = buildNoInformationAnswer(toolResult, plan);
        AiChatMessage assistantMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.ASSISTANT,
                fallbackAnswer
        );
        AiChatMessageDebugResponse debug = persistDebug(
                assistantMessage,
                buildDebugTrace(
                        toolResult,
                        plan,
                        toolResult.status() == AiToolResultStatus.ERROR ? AiAnswerSource.ERROR : AiAnswerSource.NO_MATCH,
                        plan != null && plan.wantsRag(),
                        "No tool/RAG path returned enough information.",
                        toolResult.status() == AiToolResultStatus.ERROR ? fallbackAnswer : null
                )
        );

        return new AiChatResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                request.question(),
                fallbackAnswer,
                false,
                sources == null ? 0 : sources.size(),
                sources == null ? List.of() : sources,
                toolEvidence == null ? List.of() : toolEvidence,
                debug
        );
    }

    private String buildNoInformationAnswer(AiToolResult toolResult, AiExecutionPlan plan) {
        if (toolResult.status() == AiToolResultStatus.NOT_APPLICABLE) {
            return """
                    No encontré información relacionada con lo que preguntaste en los documentos indexados/RAG.

                    Puedes intentar:
                    - Preguntar con otro nombre o una frase más específica.
                    - Verificar que el documento esté cargado, procesado e indexado para IA.
                    - Confirmar que la información exista en TAMIAS.
                    """.trim();
        }

        if (toolResult.answer() == null) {
            return """
                    No encontré información relacionada con lo que preguntaste.

                    Revisé los documentos indexados/RAG, pero no encontré contenido relacionado.

                    """.trim();
        }

        return """
                No encontré información relacionada con lo que preguntaste.

                Revisé:
                - Datos del sistema: la tool aplicable no encontró registros suficientes.
                - Documentos indexados/RAG: no encontré contenido relacionado.

                Detalle de datos del sistema:
                %s

                Puedes intentar preguntar con otro nombre, revisar si el documento está procesado/indexado, o confirmar que la información exista en TAMIAS.
                """.formatted(toolResult.answer().answer()).trim();
    }


    private AiChatMessageDebugResponse persistDebug(AiChatMessage assistantMessage, AiToolDebugTrace trace) {
        debugTraceService.saveTrace(assistantMessage, trace);
        return debugTraceService.findDebugForMessageIfEnabled(assistantMessage).orElse(null);
    }

    private AiToolDebugTrace buildDebugTrace(
            AiToolResult toolResult,
            AiExecutionPlan plan,
            AiAnswerSource answerSource,
            boolean ragUsed,
            String fallbackReason,
            String errorMessage
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (toolResult != null && toolResult.params() != null) {
            params.putAll(toolResult.params());
        }
        if (plan != null) {
            params.put("planDecision", plan.safeDecision().name());
            params.put("planConfidence", plan.confidence());
            params.put("planLlmGenerated", plan.llmGenerated());
        }

        List<String> toolNames = toolNamesFromResult(toolResult);
        String toolName = toolNames.isEmpty() ? null : toolNames.get(0);
        String handler = toolResult != null ? toolResult.handler() : null;
        if ((handler == null || handler.isBlank()) && toolName != null && toolName.startsWith("assistant.llm")) {
            handler = "AiPlanningService";
        }

        return new AiToolDebugTrace(
                handler,
                toolName,
                toolNames,
                params,
                ragUsed,
                answerSource,
                planSummary(plan),
                fallbackReason,
                errorMessage
        );
    }

    private List<String> toolNamesFromResult(AiToolResult toolResult) {
        if (toolResult == null) {
            return List.of();
        }
        if (toolResult.toolNames() != null && !toolResult.toolNames().isEmpty()) {
            return toolResult.toolNames();
        }
        if (toolResult.answer() == null || toolResult.answer().evidence() == null) {
            return List.of();
        }
        return toolResult.answer().evidence().stream()
                .map(AiToolEvidenceResponse::toolName)
                .filter(toolName -> toolName != null && !toolName.isBlank())
                .distinct()
                .toList();
    }

    private AiAnswerSource answerSourceForToolAnswer(AiToolResult result, AiToolAnswer backendAnswer, String finalAnswer) {
        if (result.status() == AiToolResultStatus.ERROR) {
            return AiAnswerSource.ERROR;
        }
        if (backendAnswer == null) {
            return AiAnswerSource.NO_MATCH;
        }
        return Objects.equals(finalAnswer, backendAnswer.answer())
                ? AiAnswerSource.BACKEND_DIRECT
                : AiAnswerSource.LLM_COMPOSED;
    }

    private AiToolResult toolResultForRagContext(AiToolResult toolResult, AiExecutionPlan plan) {
        if (toolResult == null || toolResult.status() == AiToolResultStatus.NOT_APPLICABLE || plan == null) {
            return AiToolResult.notApplicable();
        }
        if (plan.ragOnly()) {
            return AiToolResult.notApplicable();
        }
        if (plan.wantsBoth()) {
            return toolResult;
        }
        if (toolResult.status() == AiToolResultStatus.EMPTY || toolResult.status() == AiToolResultStatus.ERROR) {
            return toolResult;
        }
        return AiToolResult.notApplicable();
    }

    private List<AiToolEvidenceResponse> toolEvidenceForResponse(AiToolResult toolResult, AiExecutionPlan plan) {
        if (toolResult == null || toolResult.answer() == null || plan == null || plan.ragOnly()) {
            return List.of();
        }
        if (plan.wantsBoth() || toolResult.status() == AiToolResultStatus.EMPTY || toolResult.status() == AiToolResultStatus.ERROR) {
            return toolResult.answer().evidence();
        }
        return List.of();
    }

    private String planSummary(AiExecutionPlan plan) {
        if (plan == null) {
            return "No LLM planning metadata.";
        }
        return "decision=" + plan.safeDecision()
                + "; confidence=" + plan.confidence()
                + "; llmGenerated=" + plan.llmGenerated()
                + "; reason=" + plan.reason();
    }

    private String buildToolFallbackContext(AiToolResult toolResult) {
        if (toolResult == null || toolResult.status() == AiToolResultStatus.NOT_APPLICABLE || toolResult.answer() == null) {
            return "";
        }
        return "- Estado de tool: " + toolResult.status() + System.lineSeparator()
                + "- Respuesta de tool: " + toolResult.answer().answer();
    }

    private List<Document> searchSimilarDocuments(
            String question,
            UUID propertyId,
            Integer topK,
            Double similarityThreshold
    ) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(topK != null ? topK : defaultTopK)
                .similarityThreshold(similarityThreshold != null ? similarityThreshold : defaultSimilarityThreshold)
                .filterExpression(buildFilterExpression(propertyId))
                .build());
    }

    private String buildFilterExpression(UUID propertyId) {
        String organizationId = currentUserService.getCurrentOrganizationId().toString();
        if (propertyId == null) {
            return "%s == '%s'".formatted(RagMetadataKeys.ORGANIZATION_ID, organizationId);
        }

        return "%s == '%s' && %s == '%s'".formatted(
                RagMetadataKeys.ORGANIZATION_ID,
                organizationId,
                RagMetadataKeys.PROPERTY_ID,
                propertyId.toString()
        );
    }

    private String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int index = 0; index < documents.size(); index++) {
            Document document = documents.get(index);
            Map<String, Object> metadata = document.getMetadata();

            context.append("[S").append(index + 1).append("] ")
                    .append(metadata.getOrDefault(RagMetadataKeys.DOCUMENT_TITLE, "Unknown document"))
                    .append(" | type: ").append(metadata.getOrDefault(RagMetadataKeys.DOCUMENT_TYPE, "UNKNOWN"))
                    .append(" | chunk: ").append(metadata.getOrDefault(RagMetadataKeys.CHUNK_INDEX, "?"))
                    .append(System.lineSeparator())
                    .append(document.getText())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return context.toString();
    }

    private List<AiSourceResponse> toSourceResponses(List<Document> documents) {
        return java.util.stream.IntStream.range(0, documents.size())
                .mapToObj(index -> toSourceResponse(index + 1, documents.get(index)))
                .toList();
    }

    private AiSourceResponse toSourceResponse(int sourceNumber, Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return new AiSourceResponse(
                "S" + sourceNumber,
                document.getId(),
                parseUuid(metadata.get(RagMetadataKeys.DOCUMENT_ID)),
                parseUuid(metadata.get(RagMetadataKeys.CHUNK_ID)),
                parseUuid(metadata.get(RagMetadataKeys.PROPERTY_ID)),
                asString(metadata.get(RagMetadataKeys.DOCUMENT_TITLE)),
                asString(metadata.get(RagMetadataKeys.DOCUMENT_TYPE)),
                parseInteger(metadata.get(RagMetadataKeys.CHUNK_INDEX)),
                document.getScore(),
                buildExcerpt(document.getText())
        );
    }

    private String buildExcerpt(String content) {
        if (content == null) {
            return null;
        }

        String normalized = normalizeExcerptWhitespace(content);
        return normalized.length() <= MAX_SOURCE_EXCERPT_LENGTH
                ? normalized
                : normalized.substring(0, MAX_SOURCE_EXCERPT_LENGTH).trim() + "...";
    }

    private String normalizeExcerptWhitespace(String content) {
        StringBuilder builder = new StringBuilder(content.length());
        boolean previousWasSpace = false;
        int consecutiveNewLines = 0;

        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (current == '\r') {
                continue;
            }
            if (current == '\n') {
                if (consecutiveNewLines < 2) {
                    builder.append('\n');
                }
                consecutiveNewLines++;
                previousWasSpace = false;
                continue;
            }
            consecutiveNewLines = 0;
            if (current == ' ' || current == '\t') {
                if (!previousWasSpace) {
                    builder.append(' ');
                    previousWasSpace = true;
                }
                continue;
            }
            builder.append(current);
            previousWasSpace = false;
        }

        return trimWhitespace(builder.toString());
    }

    private String trimWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && Character.isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private UUID parseUuid(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return UUID.fromString(value.toString());
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
