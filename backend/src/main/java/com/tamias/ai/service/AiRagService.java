package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiChatResponse;
import com.tamias.ai.dto.AiSearchRequest;
import com.tamias.ai.dto.AiSearchResponse;
import com.tamias.ai.dto.AiSourceResponse;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatSession;
import com.tamias.ai.enums.AiChatMessageRole;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.AiToolCallingService;
import com.tamias.ai.tool.AiToolResult;
import com.tamias.ai.tool.AiToolResultStatus;
import com.tamias.security.service.CurrentUserService;
import java.util.List;
import java.util.Map;
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
    private final AiToolCallingService toolCallingService;
    private final int defaultTopK;
    private final double defaultSimilarityThreshold;

    public AiRagService(
            VectorStore vectorStore,
            ChatModel chatModel,
            CurrentUserService currentUserService,
            AiChatSessionService chatSessionService,
            AiToolCallingService toolCallingService,
            @Value("${tamias.ai.default-top-k:10}") int defaultTopK,
            @Value("${tamias.ai.default-similarity-threshold:0.30}") double defaultSimilarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.create(chatModel);
        this.currentUserService = currentUserService;
        this.chatSessionService = chatSessionService;
        this.toolCallingService = toolCallingService;
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
        if (toolResult.shouldRespondImmediately()) {
            return persistToolAnswer(session, userMessage, request, toolResult.answer());
        }

        UUID effectivePropertyId = request.propertyId() != null
                ? request.propertyId()
                : (session.getProperty() != null ? session.getProperty().getId() : null);

        List<Document> matches = searchSimilarDocuments(
                request.question(),
                effectivePropertyId,
                request.topK(),
                request.similarityThreshold()
        );
        List<AiSourceResponse> sources = toSourceResponses(matches);
        List<AiToolEvidenceResponse> toolEvidence = toolResult.answerOptional()
                .map(AiToolAnswer::evidence)
                .orElse(List.of());

        if (matches.isEmpty()) {
            String fallbackAnswer = buildNoInformationAnswer(toolResult);
            AiChatMessage assistantMessage = chatSessionService.saveMessage(
                    session,
                    AiChatMessageRole.ASSISTANT,
                    fallbackAnswer
            );

            return new AiChatResponse(
                    session.getId(),
                    userMessage.getId(),
                    assistantMessage.getId(),
                    request.question(),
                    fallbackAnswer,
                    false,
                    0,
                    sources,
                    toolEvidence
            );
        }

        String answer = buildRagAnswer(request.question(), matches, toolResult);

        AiChatMessage assistantMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.ASSISTANT,
                answer
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
                toolEvidence
        );
    }

    private String buildRagAnswer(String question, List<Document> matches, AiToolResult toolResult) {
        String systemPrompt = """
                Eres TAMIAS, un asistente para administración de propiedades, alojamientos, mantenimiento, reservaciones y documentos internos.

                Reglas estrictas:
                1. Responde en el mismo idioma de la pregunta del usuario.
                2. Usa únicamente el CONTEXTO proporcionado.
                3. No inventes datos, reglas, fechas, costos, nombres ni recomendaciones que no aparezcan en el contexto.
                4. Si la respuesta no está en el contexto, dilo claramente.
                5. Cuando sí respondas, cita las fuentes usando el formato [S1], [S2], etc.
                6. Sé claro, práctico y natural; evita sonar como plantilla repetida.
                7. Si el contexto tiene reglas, tareas o recomendaciones, sepáralas en secciones simples.
                8. Si el usuario pide crear, editar, eliminar o notificar, indícale que esta versión del asistente es de consulta.
                """;

        String toolFallbackContext = buildToolFallbackContext(toolResult);
        String userPrompt = toolFallbackContext.isBlank()
                ? """
                        Pregunta del usuario:
                        %s

                        CONTEXTO:
                        %s
                        """.formatted(question, buildContext(matches))
                : """
                        Pregunta del usuario:
                        %s

                        Antes de consultar documentos, TAMIAS intentó responder con datos estructurados del sistema, pero esa ruta no encontró datos suficientes:
                        %s

                        Usa el CONTEXTO documental de abajo para responder la pregunta. No repitas como respuesta final que la tool no encontró datos si el contexto documental sí contiene la respuesta.

                        CONTEXTO:
                        %s
                        """.formatted(question, toolFallbackContext, buildContext(matches));

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
            AiToolAnswer answer
    ) {
        AiChatMessage assistantMessage = chatSessionService.saveMessage(
                session,
                AiChatMessageRole.ASSISTANT,
                answer.answer()
        );

        return new AiChatResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                request.question(),
                answer.answer(),
                answer.grounded(),
                0,
                List.of(),
                answer.evidence()
        );
    }

    private String buildNoInformationAnswer(AiToolResult toolResult) {
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
