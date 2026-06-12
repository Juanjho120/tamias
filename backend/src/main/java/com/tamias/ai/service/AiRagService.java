package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiChatResponse;
import com.tamias.ai.dto.AiSearchRequest;
import com.tamias.ai.dto.AiSearchResponse;
import com.tamias.ai.dto.AiSourceResponse;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatSession;
import com.tamias.ai.enums.AiChatMessageRole;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.AiToolCallingService;
import com.tamias.security.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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
        List<org.springframework.ai.document.Document> matches = searchSimilarDocuments(
            request.question(), request.propertyId(), request.topK(), request.similarityThreshold());

        List<AiSourceResponse> sources = toSourceResponses(matches);

        return new AiSearchResponse(request.question(), sources.size(), sources);
    }

    public AiChatResponse chat(AiChatRequest request) {
        AiChatSession session = chatSessionService.getOrCreateSession(
            request.chatSessionId(), request.propertyId(), request.title(), request.question());

        AiChatMessage userMessage = chatSessionService.saveMessage(session, AiChatMessageRole.USER, request.question());

        Optional<AiToolAnswer> toolAnswer = toolCallingService.tryHandle(request);
        if (toolAnswer.isPresent()) {
            AiToolAnswer answer = toolAnswer.get();
            AiChatMessage assistantMessage = chatSessionService.saveMessage(session, AiChatMessageRole.ASSISTANT, answer.answer());

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

        UUID effectivePropertyId = request.propertyId() != null
            ? request.propertyId()
            : (session.getProperty() != null ? session.getProperty().getId() : null);

        List<org.springframework.ai.document.Document> matches = searchSimilarDocuments(
            request.question(), effectivePropertyId, request.topK(), request.similarityThreshold());

        List<AiSourceResponse> sources = toSourceResponses(matches);

        if (matches.isEmpty()) {
            String fallbackAnswer = """
                No encontré información suficiente en los documentos indexados para responder eso.

                Puedes intentar:
                - Subir o procesar un documento relacionado.
                - Indexar el documento para IA.
                - Preguntar de forma más específica.
                """.trim();

            AiChatMessage assistantMessage = chatSessionService.saveMessage(session, AiChatMessageRole.ASSISTANT, fallbackAnswer);

            return new AiChatResponse(
                session.getId(),
                userMessage.getId(),
                assistantMessage.getId(),
                request.question(),
                fallbackAnswer,
                false,
                0,
                sources,
                List.of()
            );
        }

        String answer = chatClient.prompt()
            .system("""
                Eres TAMIAS, un asistente para administración de propiedades, alojamientos, mantenimiento, reservaciones y documentos internos.

                Reglas estrictas:
                1. Responde en el mismo idioma de la pregunta del usuario.
                2. Usa únicamente el CONTEXTO proporcionado.
                3. No inventes datos, reglas, fechas, costos, nombres ni recomendaciones que no aparezcan en el contexto.
                4. Si la respuesta no está en el contexto, dilo claramente.
                5. Cuando sí respondas, cita las fuentes usando el formato [S1], [S2], etc.
                6. Sé claro, práctico y directo.
                7. Si el contexto tiene reglas o recomendaciones, sepáralas en secciones simples.
                8. Si el usuario pide crear, editar, eliminar o notificar, indícale que esta versión del asistente es de consulta.
                """)
            .user("""
                Pregunta del usuario:

                %s

                CONTEXTO:

                %s
                """.formatted(request.question(), buildContext(matches)))
            .call()
            .content();

        AiChatMessage assistantMessage = chatSessionService.saveMessage(session, AiChatMessageRole.ASSISTANT, answer);

        return new AiChatResponse(
            session.getId(),
            userMessage.getId(),
            assistantMessage.getId(),
            request.question(),
            answer,
            true,
            sources.size(),
            sources,
            List.of()
        );
    }

    private List<org.springframework.ai.document.Document> searchSimilarDocuments(
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

    private String buildContext(List<org.springframework.ai.document.Document> documents) {
        StringBuilder context = new StringBuilder();

        for (int index = 0; index < documents.size(); index++) {
            org.springframework.ai.document.Document document = documents.get(index);
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

    private List<AiSourceResponse> toSourceResponses(List<org.springframework.ai.document.Document> documents) {
        return java.util.stream.IntStream.range(0, documents.size())
            .mapToObj(index -> toSourceResponse(index + 1, documents.get(index)))
            .toList();
    }

    private AiSourceResponse toSourceResponse(int sourceNumber, org.springframework.ai.document.Document document) {
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

        String normalized = content.replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\\n\\n")
            .trim();

        return normalized.length() <= MAX_SOURCE_EXCERPT_LENGTH
            ? normalized
            : normalized.substring(0, MAX_SOURCE_EXCERPT_LENGTH).trim() + "...";
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
