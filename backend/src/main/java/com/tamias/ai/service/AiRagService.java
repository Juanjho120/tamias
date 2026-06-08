package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiChatResponse;
import com.tamias.ai.dto.AiSearchRequest;
import com.tamias.ai.dto.AiSearchResponse;
import com.tamias.ai.dto.AiSourceResponse;
import com.tamias.security.service.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final CurrentUserService currentUserService;
    private final int defaultTopK;
    private final double defaultSimilarityThreshold;

    public AiRagService(
            VectorStore vectorStore,
            ChatModel chatModel,
            CurrentUserService currentUserService,
            @Value("${tamias.ai.default-top-k:5}") int defaultTopK,
            @Value("${tamias.ai.default-similarity-threshold:0.70}") double defaultSimilarityThreshold
    ) {
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.create(chatModel);
        this.currentUserService = currentUserService;
        this.defaultTopK = defaultTopK;
        this.defaultSimilarityThreshold = defaultSimilarityThreshold;
    }

    public AiSearchResponse search(AiSearchRequest request) {
        List<org.springframework.ai.document.Document> matches = searchSimilarDocuments(
                request.question(),
                request.propertyId(),
                request.topK(),
                request.similarityThreshold()
        );

        return new AiSearchResponse(
                request.question(),
                matches.stream().map(this::toSourceResponse).toList()
        );
    }

    public AiChatResponse chat(AiChatRequest request) {
        List<org.springframework.ai.document.Document> matches = searchSimilarDocuments(
                request.question(),
                request.propertyId(),
                request.topK(),
                request.similarityThreshold()
        );

        String context = buildContext(matches);

        String answer = chatClient.prompt()
                .system("""
                        You are TAMIAS, an assistant for property management.
                        Answer only using the provided context.
                        If the answer is not in the context, say that the information is not available in the indexed documents.
                        Be concise and include source references using the format [documentTitle | chunkIndex].
                        """)
                .user("""
                        Question:
                        %s

                        Context:
                        %s
                        """.formatted(request.question(), context))
                .call()
                .content();

        return new AiChatResponse(
                request.question(),
                answer,
                matches.stream().map(this::toSourceResponse).toList()
        );
    }

    private List<org.springframework.ai.document.Document> searchSimilarDocuments(
            String question,
            UUID propertyId,
            Integer topK,
            Double similarityThreshold
    ) {
        String filterExpression = buildFilterExpression(propertyId);

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK != null ? topK : defaultTopK)
                        .similarityThreshold(similarityThreshold != null ? similarityThreshold : defaultSimilarityThreshold)
                        .filterExpression(filterExpression)
                        .build()
        );
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

        for (org.springframework.ai.document.Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();

            context.append("[")
                    .append(metadata.getOrDefault(RagMetadataKeys.DOCUMENT_TITLE, "Unknown document"))
                    .append(" | chunk ")
                    .append(metadata.getOrDefault(RagMetadataKeys.CHUNK_INDEX, "?"))
                    .append("]")
                    .append(System.lineSeparator())
                    .append(document.getText())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }

        return context.toString();
    }

    private AiSourceResponse toSourceResponse(org.springframework.ai.document.Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return new AiSourceResponse(
                document.getId(),
                parseUuid(metadata.get(RagMetadataKeys.DOCUMENT_ID)),
                parseUuid(metadata.get(RagMetadataKeys.CHUNK_ID)),
                parseUuid(metadata.get(RagMetadataKeys.PROPERTY_ID)),
                asString(metadata.get(RagMetadataKeys.DOCUMENT_TITLE)),
                asString(metadata.get(RagMetadataKeys.DOCUMENT_TYPE)),
                parseInteger(metadata.get(RagMetadataKeys.CHUNK_INDEX)),
                document.getScore(),
                document.getText()
        );
    }

    private UUID parseUuid(Object value) {
        if (value == null) {
            return null;
        }

        String text = value.toString();

        if (text.isBlank()) {
            return null;
        }

        return UUID.fromString(text);
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
