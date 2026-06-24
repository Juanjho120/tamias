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

public abstract class AiMaintenancePropertyCatalogReadSupport extends AiReservationSupplyTaskReadSupport {

    protected AiMaintenancePropertyCatalogReadSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected List<Map<String, Object>> maintenanceRows(UUID organizationId, String search, String propertySearch, String categoryOrTypeSearch, String status, String itemSearch, int limit) {
        return query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.description,
                       mc.name AS category_name,
                       mt.name AS type_name,
                       mr.performed_at,
                       mr.scheduled_at,
                       mr.cost,
                       mr.status,
                       COUNT(DISTINCT mri.id) AS item_count,
                       COUNT(DISTINCT img.id) AS image_count
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = mr.maintenance_type_id
                LEFT JOIN maintenance_record_items mri ON mri.maintenance_record_id = mr.id
                                                     AND mri.organization_id = mr.organization_id
                LEFT JOIN maintenance_record_images img ON img.maintenance_record_id = mr.id
                                                       AND img.organization_id = mr.organization_id
                                                       AND img.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND (CAST(:status AS TEXT) IS NULL OR mr.status = CAST(:status AS TEXT))
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(CONCAT_WS(' ', mr.title, mr.description, mc.name, mt.name, p.name)) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', mr.title, mr.description, mc.name, mt.name, p.name)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:propertySearch AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', p.name, p.address, p.description)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:categoryOrTypeSearch AS TEXT) IS NULL
                       OR NOT EXISTS (
                           SELECT 1
                           FROM unnest(string_to_array(CAST(:categoryOrTypeSearch AS TEXT), ' ')) AS token(value)
                           WHERE token.value <> ''
                             AND LOWER(CONCAT_WS(' ', mc.name, mt.name)) NOT LIKE CONCAT('%', token.value, '%')
                       )
                  )
                  AND (
                       CAST(:itemSearch AS TEXT) IS NULL
                       OR EXISTS (
                           SELECT 1
                           FROM maintenance_record_items item_filter
                           WHERE item_filter.maintenance_record_id = mr.id
                             AND item_filter.organization_id = mr.organization_id
                             AND NOT EXISTS (
                                 SELECT 1
                                 FROM unnest(string_to_array(CAST(:itemSearch AS TEXT), ' ')) AS token(value)
                                 WHERE token.value <> ''
                                   AND LOWER(CONCAT_WS(' ', item_filter.item_name_snapshot, item_filter.notes)) NOT LIKE CONCAT('%', token.value, '%')
                             )
                       )
                  )
                GROUP BY mr.id, p.name, mr.title, mr.description, mc.name, mt.name, mr.performed_at, mr.scheduled_at, mr.cost, mr.status
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                    q.setParameter("propertySearch", propertySearch);
                    q.setParameter("categoryOrTypeSearch", categoryOrTypeSearch);
                    q.setParameter("status", status);
                    q.setParameter("itemSearch", itemSearch);
                    q.setParameter("limit", limit);
                }, "id", "propertyName", "title", "description", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status", "itemCount", "imageCount");
    }

    protected AiToolAnswer maintenanceRowsAnswer(List<Map<String, Object>> rows, String toolName, String label, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | categoría: ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("typeName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("performedAt"))));
            if (!value(row.get("cost")).isBlank()) {
                answer.append(" | costo: ").append(formatMoney(row.get("cost")));
            }
            answer.append(" | items: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d maintenance rows found.".formatted(rows.size()), rows);
    }

    protected AiToolAnswer propertiesByStatus(String status, String toolName, String label) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = propertySearchRows(organizationId, null, status, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré propiedades con estado " + status + ".",
                    toolName,
                    label,
                    "No properties found for status " + status + ".",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(status.equals("ACTIVE")
                ? "Estas son tus propiedades activas:"
                : "Estas son tus propiedades inactivas:");
        appendPropertyList(answer, rows);
        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d properties found for status %s.".formatted(rows.size(), status),
                rows
        );
    }

    protected List<Map<String, Object>> propertySearchRows(UUID organizationId, String search, String status, int limit) {
        List<Map<String, Object>> candidates = query("""
                SELECT p.id, p.name, p.status, p.address, p.description
                FROM properties p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND (CAST(:status AS TEXT) IS NULL OR p.status = CAST(:status AS TEXT))
                ORDER BY p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("status", status);
                    q.setParameter("limit", Math.max(limit, 50));
                }, "id", "name", "status", "address", "description");

        if (search == null || search.isBlank()) {
            return candidates.stream().limit(limit).toList();
        }

        return candidates.stream()
                .filter(row -> propertyMatchScore(row, search) > 0)
                .sorted((left, right) -> Integer.compare(propertyMatchScore(right, search), propertyMatchScore(left, search)))
                .limit(limit)
                .toList();
    }

    protected void appendPropertyList(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ")
                    .append(blankToDash(value(row.get("name"))))
                    .append(" — estado: ")
                    .append(blankToDash(value(row.get("status"))));
            String address = value(row.get("address"));
            if (!address.isBlank()) {
                answer.append(" | dirección: ").append(address);
            }
        }
    }

    protected AiToolAnswer baseCatalog(String tableName, String toolName, String label, String spanishName) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT id, name, description, status
                FROM %s
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                ORDER BY status ASC, name ASC
                LIMIT :limit
                """.formatted(tableName), q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "description", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré " + spanishName + " configurados en tu organización.",
                    toolName,
                    label,
                    "No catalog rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder("Estos son los " + spanishName + " configurados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" — estado: ").append(blankToDash(value(row.get("status"))));
            String description = value(row.get("description"));
            if (!description.isBlank()) {
                answer.append(" | ").append(description);
            }
        }
        return AiToolAnswer.of(
                answer.toString(),
                toolName,
                label,
                "%d catalog rows found.".formatted(rows.size()),
                rows
        );
    }

    protected Map<String, Object> bestPropertyMatch(List<Map<String, Object>> candidates, String search) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (search == null || search.isBlank()) {
            return candidates.get(0);
        }

        Map<String, Object> best = null;
        int bestScore = 0;
        for (Map<String, Object> row : candidates) {
            int score = propertyMatchScore(row, search);
            if (score > bestScore) {
                bestScore = score;
                best = row;
            }
        }
        return bestScore > 0 ? best : null;
    }

    protected int propertyMatchScore(Map<String, Object> row, String search) {
        String propertyName = value(row.get("name"));
        String haystack = normalize(String.join(" ",
                propertyName,
                value(row.get("address")),
                value(row.get("description"))
        ));
        String normalizedSearch = normalize(search);
        if (haystack.isBlank() || normalizedSearch.isBlank()) {
            return 0;
        }
        if (haystack.contains(normalizedSearch)) {
            return 100 + normalizedSearch.length();
        }

        List<String> searchTokens = searchTokens(normalizedSearch);
        List<String> haystackTokens = searchTokens(haystack);
        if (searchTokens.isEmpty() || haystackTokens.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (String searchToken : searchTokens) {
            if (haystackTokens.stream().anyMatch(haystackToken -> tokenMatches(searchToken, haystackToken))) {
                score++;
            }
        }

        int requiredMatches = searchTokens.size() <= 2 ? searchTokens.size() : Math.max(2, searchTokens.size() - 1);
        return score >= requiredMatches ? score : 0;
    }

    protected List<String> searchTokens(String value) {
        return splitWords(normalize(value)).stream()
                .filter(token -> !SEARCH_STOP_WORDS.contains(token))
                .toList();
    }

    protected boolean tokenMatches(String needle, String candidate) {
        if (candidate.equals(needle) || candidate.contains(needle) || needle.contains(candidate)) {
            return true;
        }
        return needle.length() >= 5 && candidate.length() >= 5 && levenshteinDistanceAtMostOne(needle, candidate);
    }

    protected boolean levenshteinDistanceAtMostOne(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 1) {
            return false;
        }
        int i = 0;
        int j = 0;
        int edits = 0;
        while (i < left.length() && j < right.length()) {
            if (left.charAt(i) == right.charAt(j)) {
                i++;
                j++;
                continue;
            }
            edits++;
            if (edits > 1) {
                return false;
            }
            if (left.length() > right.length()) {
                i++;
            } else if (right.length() > left.length()) {
                j++;
            } else {
                i++;
                j++;
            }
        }
        return edits + (left.length() - i) + (right.length() - j) <= 1;
    }

    protected String indentCatalogAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return "- Sin datos configurados.";
        }
        return splitLines(answer).stream()
                .filter(line -> line.trim().startsWith("-"))
                .map(line -> "   " + line.trim())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
