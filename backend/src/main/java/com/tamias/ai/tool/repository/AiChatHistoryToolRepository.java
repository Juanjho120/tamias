package com.tamias.ai.tool.repository;

import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class AiChatHistoryToolRepository extends AiReadOnlyToolSupport {

    public AiChatHistoryToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer aiChatRecentSessions(UUID excludedSessionId) {
        List<Map<String, Object>> rows = aiChatSessionRows(null, null, excludedSessionId, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré sesiones de chat IA tuyas.",
                    "aiChat.recentSessions",
                    "AI chat recent sessions",
                    "No AI chat sessions were found for the current user.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estas son las sesiones recientes del asistente IA:");
        appendAiChatSessionRows(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.recentSessions",
                "AI chat recent sessions",
                "%d recent AI chat sessions were consulted.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer aiChatLastPreviousSession(UUID excludedSessionId) {
        List<Map<String, Object>> rows = aiChatSessionRows(null, null, excludedSessionId, 1);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré una conversación anterior del asistente IA fuera de esta conversación.",
                    "aiChat.recentSessions",
                    "AI chat last previous session",
                    "No previous AI chat session was found outside the current session.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("La última conversación que hemos tenido, excluyendo esta conversación:");
        appendAiChatSessionRows(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.recentSessions",
                "AI chat last previous session",
                "The latest previous AI chat session was consulted.",
                rows
        );
    }

    public AiToolAnswer aiChatSearchHistory(String userQuestion, UUID excludedSessionId) {
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "busca", "buscar", "historial", "chat", "chats", "conversacion", "conversaciones",
                "sesion", "sesiones", "ia", "asistente", "pregunta", "preguntas", "mensaje", "mensajes",
                "hemos", "hablado", "hablamos", "hablar", "antes", "sobre", "relacionado", "relacionados", "si", "de"
        ));
        List<Map<String, Object>> rows = aiChatSessionsContaining(search, excludedSessionId, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré sesiones en el historial del asistente IA."
                            : "No encontré sesiones tuyas donde hayamos hablado de “" + search + "”.",
                    "aiChat.searchHistory",
                    "AI chat history search",
                    "No AI chat sessions matched the search criteria.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estas son las sesiones recientes del historial del asistente IA:"
                : "Estas son las sesiones donde hablamos de " + search + ":");
        appendAiChatSessionRows(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.searchHistory",
                "AI chat history search",
                "%d AI chat sessions matched the search criteria.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer aiChatRecentMessages(UUID excludedSessionId) {
        List<Map<String, Object>> rows = aiChatMessageRows(null, null, excludedSessionId, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré mensajes recientes tuyos del asistente IA.",
                    "aiChat.recentMessages",
                    "AI chat recent messages",
                    "No recent AI chat messages were found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estos son los mensajes recientes del historial del asistente IA:");
        appendAiChatMessageRows(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.recentMessages",
                "AI chat recent messages",
                "%d recent AI chat messages were consulted.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer aiChatRecentUserQuestions(UUID excludedSessionId) {
        List<Map<String, Object>> rows = aiChatMessageRows(null, "USER", excludedSessionId, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré preguntas anteriores tuyas al asistente IA fuera de esta conversación.",
                    "aiChat.recentMessages",
                    "AI chat recent user questions",
                    "No previous user questions were found outside the current session.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estas son las preguntas recientes que le hiciste al asistente IA, excluyendo esta conversación:");
        appendAiChatUserQuestionRowsGroupedBySession(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.recentMessages",
                "AI chat recent user questions",
                "%d recent user questions were consulted outside the current session.".formatted(rows.size()),
                rows
        );
    }

    private void appendAiChatUserQuestionRowsGroupedBySession(StringBuilder answer, List<Map<String, Object>> rows) {
        String currentSessionTitle = null;
        for (Map<String, Object> row : rows) {
            String sessionTitle = blankToDash(value(row.get("sessionTitle")));
            if (!sessionTitle.equals(currentSessionTitle)) {
                currentSessionTitle = sessionTitle;
                answer.append(System.lineSeparator())
                        .append("Sesión ").append(currentSessionTitle);
            }
            answer.append(System.lineSeparator())
                    .append(formatDateTime(row.get("createdAt")))
                    .append(" - [").append(blankToDash(value(row.get("role")))).append("] ")
                    .append(blankToDash(firstLine(value(row.get("contentExcerpt")))));
        }
    }

    public AiToolAnswer aiChatSessionsByProperty(String userQuestion, UUID excludedSessionId) {
        String propertySearch = nullableSearch(extractSearchText(
                userQuestion,
                "chat", "chats", "conversacion", "conversaciones", "sesion", "sesiones", "ia", "asistente",
                "propiedad", "propiedades", "alojamiento", "alojamientos", "casa", "casas", "bungalow", "bungalows", "sobre", "relacionado", "relacionados"
        ));
        List<Map<String, Object>> rows = aiChatSessionRows(propertySearch, null, excludedSessionId, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    propertySearch == null
                            ? "No encontré sesiones de chat IA asociadas a propiedades."
                            : "No encontré sesiones de chat IA para una propiedad relacionada con “" + propertySearch + "”.",
                    "aiChat.sessionsByProperty",
                    "AI chat sessions by property",
                    "No AI chat sessions matched the requested property.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(propertySearch == null
                ? "Estas son las sesiones de chat IA asociadas a propiedades:"
                : "Estas son las sesiones de chat IA para propiedades relacionadas con “" + propertySearch + "”:");
        appendAiChatSessionRows(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.sessionsByProperty",
                "AI chat sessions by property",
                "%d AI chat sessions by property were consulted.".formatted(rows.size()),
                rows
        );
    }

    public AiToolAnswer aiChatCurrentSessionSummary(UUID chatSessionId) {
        if (chatSessionId == null) {
            return AiToolAnswer.of(
                    "No tengo una sesión de chat activa para resumir. Abre una conversación existente o envía una pregunta dentro de una sesión y puedo resumirla.",
                    "aiChat.currentSessionSummary",
                    "AI chat current session summary",
                    "No current AI chat session id was provided.",
                    List.of()
            );
        }

        List<Map<String, Object>> rows = aiChatSessionRows(null, chatSessionId, 1);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré la sesión de chat IA actual entre tus conversaciones.",
                    "aiChat.currentSessionSummary",
                    "AI chat current session summary",
                    "The current AI chat session was not found for the current user.",
                    List.of()
            );
        }

        List<Map<String, Object>> messages = aiChatMessagesBySession(chatSessionId, 12);
        StringBuilder answer = new StringBuilder("Resumen de la sesión actual del asistente IA:");
        appendAiChatSessionRows(answer, rows);
        if (messages.isEmpty()) {
            answer.append(System.lineSeparator()).append("No encontré mensajes asociados a esta sesión.");
        } else {
            answer.append(System.lineSeparator()).append("Mensajes recientes de esta sesión:");
            appendAiChatTimelineRows(answer, messages);
        }

        List<Map<String, Object>> evidenceRows = new ArrayList<>(rows);
        evidenceRows.addAll(messages);
        return AiToolAnswer.of(
                answer.toString(),
                "aiChat.currentSessionSummary",
                "AI chat current session summary",
                "Current AI chat session metadata and recent messages were consulted.",
                evidenceRows
        );
    }

    private List<Map<String, Object>> aiChatSessionsContaining(String search, UUID excludedSessionId, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();
        StringBuilder sql = new StringBuilder("""
                SELECT s.id,
                       s.title,
                       p.name AS property_name,
                       TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS created_by_name,
                       s.created_at,
                       s.updated_at,
                       COUNT(m.id) AS message_count,
                       MAX(m.created_at) AS last_message_at
                FROM ai_chat_sessions s
                LEFT JOIN properties p ON p.id = s.property_id
                                      AND p.organization_id = s.organization_id
                LEFT JOIN users u ON u.id = s.created_by
                LEFT JOIN ai_chat_messages m ON m.chat_session_id = s.id
                                            AND m.organization_id = s.organization_id
                WHERE s.organization_id = :organizationId
                  AND s.created_by = :currentUserId
                """);
        if (excludedSessionId != null) {
            sql.append("  AND s.id <> :excludedSessionId\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', s.title, COALESCE(p.name, ''), COALESCE((
                            SELECT STRING_AGG(m2.content, ' ' ORDER BY m2.created_at ASC)
                            FROM ai_chat_messages m2
                            WHERE m2.chat_session_id = s.id
                              AND m2.organization_id = s.organization_id
                        ), ''))), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY s.id, s.title, p.name, u.first_name, u.last_name, s.created_at, s.updated_at
                ORDER BY COALESCE(MAX(m.created_at), s.updated_at, s.created_at) DESC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("currentUserId", currentUserId);
            if (excludedSessionId != null) q.setParameter("excludedSessionId", excludedSessionId);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "title", "propertyName", "createdByName", "createdAt", "updatedAt", "messageCount", "lastMessageAt");
    }

    public AiToolAnswer aiChatUsageSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT s.id) AS session_count,
                       COUNT(m.id) AS message_count,
                       COUNT(CASE WHEN m.role = 'USER' THEN 1 END) AS user_message_count,
                       COUNT(CASE WHEN m.role = 'ASSISTANT' THEN 1 END) AS assistant_message_count,
                       COUNT(DISTINCT s.property_id) AS property_count,
                       MIN(s.created_at) AS first_session_at,
                       MAX(COALESCE(m.created_at, s.updated_at, s.created_at)) AS last_activity_at
                FROM ai_chat_sessions s
                LEFT JOIN ai_chat_messages m ON m.chat_session_id = s.id
                                            AND m.organization_id = s.organization_id
                WHERE s.organization_id = :organizationId
                  AND s.created_by = :currentUserId
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("currentUserId", currentUserId);
                },
                "sessionCount", "messageCount", "userMessageCount", "assistantMessageCount", "propertyCount", "firstSessionAt", "lastActivityAt");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Uso de tu historial de chat IA:\n"
                + "- Sesiones: " + blankToDash(value(row.get("sessionCount"))) + "\n"
                + "- Mensajes totales: " + blankToDash(value(row.get("messageCount"))) + "\n"
                + "- Mensajes de usuarios: " + blankToDash(value(row.get("userMessageCount"))) + "\n"
                + "- Respuestas del asistente: " + blankToDash(value(row.get("assistantMessageCount"))) + "\n"
                + "- Propiedades con sesiones asociadas: " + blankToDash(value(row.get("propertyCount"))) + "\n"
                + "- Primera sesión: " + formatDateTime(row.get("firstSessionAt")) + "\n"
                + "- Última actividad: " + formatDateTime(row.get("lastActivityAt"));
        return AiToolAnswer.of(
                answer,
                "aiChat.usageSummary",
                "AI chat usage summary",
                "Current user AI chat session and message counters were consulted.",
                rows
        );
    }
}
