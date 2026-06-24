package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AiChatHistoryReadSupport extends AiFileImageReadSupport {

    protected AiChatHistoryReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> aiChatSessionRows(String propertySearch, UUID sessionId, int limit) {
        return aiChatSessionRows(propertySearch, sessionId, null, limit);
    }

    protected List<Map<String, Object>> aiChatSessionRows(String propertySearch, UUID sessionId, UUID excludedSessionId, int limit) {
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
        if (sessionId != null) {
            sql.append("  AND s.id = :sessionId\n");
        }
        if (excludedSessionId != null) {
            sql.append("  AND s.id <> :excludedSessionId\n");
        }
        if (propertySearch != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', COALESCE(p.name, ''), s.title, COALESCE((
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
            if (sessionId != null) q.setParameter("sessionId", sessionId);
            if (excludedSessionId != null) q.setParameter("excludedSessionId", excludedSessionId);
            if (propertySearch != null) q.setParameter("propertySearch", propertySearch);
            q.setParameter("limit", limit);
        }, "id", "title", "propertyName", "createdByName", "createdAt", "updatedAt", "messageCount", "lastMessageAt");
    }

    protected List<Map<String, Object>> aiChatMessageRows(String search, int limit) {
        return aiChatMessageRows(search, null, null, limit);
    }

    protected List<Map<String, Object>> aiChatMessageRows(String search, String role, UUID excludedSessionId, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();
        StringBuilder sql = new StringBuilder("""
                SELECT m.id,
                       s.id AS session_id,
                       s.title AS session_title,
                       p.name AS property_name,
                       m.role,
                       LEFT(m.content, 500) AS content_excerpt,
                       m.created_at
                FROM ai_chat_messages m
                JOIN ai_chat_sessions s ON s.id = m.chat_session_id
                                       AND s.organization_id = m.organization_id
                LEFT JOIN properties p ON p.id = s.property_id
                                      AND p.organization_id = s.organization_id
                WHERE m.organization_id = :organizationId
                  AND s.created_by = :currentUserId
                """);
        if (excludedSessionId != null) {
            sql.append("  AND m.chat_session_id <> :excludedSessionId\n");
        }
        if (role != null) {
            sql.append("  AND m.role = :role\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', m.content, s.title, p.name, m.role)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("ORDER BY m.created_at DESC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("currentUserId", currentUserId);
            if (excludedSessionId != null) q.setParameter("excludedSessionId", excludedSessionId);
            if (role != null) q.setParameter("role", role);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "sessionId", "sessionTitle", "propertyName", "role", "contentExcerpt", "createdAt");
    }

    protected List<Map<String, Object>> aiChatMessagesBySession(UUID sessionId, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();
        return query("""
                SELECT *
                FROM (
                    SELECT m.id,
                           s.id AS session_id,
                           s.title AS session_title,
                           p.name AS property_name,
                           m.role,
                           LEFT(m.content, 500) AS content_excerpt,
                           m.created_at
                    FROM ai_chat_messages m
                    JOIN ai_chat_sessions s ON s.id = m.chat_session_id
                                           AND s.organization_id = m.organization_id
                    LEFT JOIN properties p ON p.id = s.property_id
                                          AND p.organization_id = s.organization_id
                    WHERE m.organization_id = :organizationId
                      AND s.created_by = :currentUserId
                      AND m.chat_session_id = :sessionId
                    ORDER BY m.created_at DESC
                    LIMIT :limit
                ) recent_messages
                ORDER BY created_at ASC
                """, q -> {
            q.setParameter("organizationId", organizationId);
            q.setParameter("currentUserId", currentUserId);
            q.setParameter("sessionId", sessionId);
            q.setParameter("limit", limit);
        }, "id", "sessionId", "sessionTitle", "propertyName", "role", "contentExcerpt", "createdAt");
    }

    protected void appendAiChatSessionRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | mensajes: ").append(blankToDash(value(row.get("messageCount"))))
                    .append(" | creada por: ").append(blankToDash(value(row.get("createdByName"))))
                    .append(" | última actividad: ").append(formatDateTime(row.get("lastMessageAt")));
        }
    }

    protected void appendAiChatMessageRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- [").append(blankToDash(value(row.get("role")))).append("] ")
                    .append(blankToDash(firstLine(value(row.get("contentExcerpt")))))
                    .append(" | sesión: ").append(blankToDash(value(row.get("sessionTitle"))))
                    .append(" | fecha: ").append(formatDateTime(row.get("createdAt")));
        }
    }

    protected void appendAiChatTimelineRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append(formatDateTime(row.get("createdAt")))
                    .append(" - [").append(blankToDash(value(row.get("role")))).append("] ")
                    .append(formatTimelineContent(value(row.get("contentExcerpt"))));
        }
    }
}
