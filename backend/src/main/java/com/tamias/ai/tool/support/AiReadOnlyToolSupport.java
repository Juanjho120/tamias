package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.dto.AiToolEvidenceResponse;
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
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public abstract class AiReadOnlyToolSupport {

    protected static final int DEFAULT_LIMIT = 10;

    protected static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "al", "algo", "actual", "actuales", "actualmente", "ahi", "aqui",
            "cargado", "cargados", "con", "cual", "cuales", "cuando", "cuanto", "cuantos",
            "da", "dame", "de", "del", "dice", "e", "el", "en", "estado", "estan", "esta",
            "este", "estos", "fue", "hay", "indexado", "indexados", "la", "las", "le",
            "lista", "listar", "lo", "los", "me", "mi", "mis", "muestra", "nombre", "o",
            "para", "por", "procesado", "procesados", "que", "quiero", "reciente", "registrada",
            "registradas", "registrado", "registrados", "se", "son", "subido", "subidos", "tengo",
            "tienes", "tipo", "tu", "un", "una", "usado", "usados", "usaron", "usan", "usa", "uso", "ver", "vez", "y"
    );

    protected final EntityManager entityManager;
    protected final CurrentUserService currentUserService;

    protected AiReadOnlyToolSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        this.entityManager = entityManager;
        this.currentUserService = currentUserService;
    }
































































































    protected AiToolAnswer scheduledMaintenanceList(String toolName, String label, String emptyOrIntro, LocalDate from, LocalDate to, String search) {
        return scheduledMaintenanceList(toolName, label, emptyOrIntro, from, to, search, DEFAULT_LIMIT);
    }

    protected AiToolAnswer scheduledMaintenanceList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
                SELECT sm.id,
                       p.name AS property_name,
                       sm.title,
                       COALESCE(mc.name, '') AS category_name,
                       COALESCE(mt.name, '') AS type_name,
                       sm.frequency,
                       sm.interval_value,
                       sm.next_due_date,
                       sm.estimated_cost,
                       sm.status
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = sm.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = sm.maintenance_type_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                """);
        if (from != null) {
            sql.append(" AND sm.next_due_date >= :fromDate\n");
        }
        if (to != null) {
            sql.append(" AND sm.next_due_date <= :toDate\n");
        }
        if (normalizedSearch != null) {
            sql.append("""
                     AND (sm.status = CAST(:search AS TEXT) OR NOT EXISTS (
                         SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                         WHERE translate(LOWER(CONCAT_WS(' ', sm.title, sm.description, p.name, mc.name, mt.name, sm.frequency, sm.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                     ))
                    """);
        }
        sql.append("""
                ORDER BY sm.next_due_date ASC, sm.title ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (from != null) {
                        q.setParameter("fromDate", Date.valueOf(from));
                    }
                    if (to != null) {
                        q.setParameter("toDate", Date.valueOf(to));
                    }
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
                    q.setParameter("limit", limit);
                }, "id", "propertyName", "title", "categoryName", "typeName", "frequency", "intervalValue", "nextDueDate", "estimatedCost", "status");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos programados que coincidan con tu pregunta.", toolName, label, "No scheduled maintenance rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("title"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("nextDueDate"))))
                    .append(" | frecuencia: ").append(blankToDash(value(row.get("frequency"))))
                    .append(" cada ").append(blankToDash(value(row.get("intervalValue"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
            String category = value(row.get("categoryName"));
            String type = value(row.get("typeName"));
            if (!category.isBlank() || !type.isBlank()) {
                answer.append(" | ").append(blankToDash(category));
                if (!type.isBlank()) {
                    answer.append(" / ").append(type);
                }
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d scheduled maintenance rows found.".formatted(rows.size()), rows);
    }



































    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search) {
        return reservationList(toolName, label, intro, from, to, search, DEFAULT_LIMIT, false);
    }

    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit, boolean currentOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
                SELECT r.id,
                       p.name AS property_name,
                       COALESCE(pl.name, '') AS platform_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.reservation_value,
                       r.status,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests,
                       COALESCE(COUNT(g.id), 0) AS guest_count
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN platforms pl ON pl.id = r.platform_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                """);
        if (currentOnly) {
            sql.append(" AND r.check_in <= :fromDate AND r.check_out > :fromDate\n");
        } else {
            if (from != null) {
                sql.append(" AND r.check_in >= :fromDate\n");
            }
            if (to != null) {
                sql.append(" AND r.check_in <= :toDate\n");
            }
        }
        if (normalizedSearch != null) {
            sql.append("""
                     AND (r.status = CAST(:search AS TEXT) OR NOT EXISTS (
                         SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                         WHERE translate(LOWER(CONCAT_WS(' ', p.name, pl.name, r.reservation_code, r.observations, r.status, g.full_name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                     ))
                    """);
        }
        sql.append("""
                GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status
                ORDER BY r.check_in ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (currentOnly || from != null) {
                        q.setParameter("fromDate", Date.valueOf(from));
                    }
                    if (!currentOnly && to != null) {
                        q.setParameter("toDate", Date.valueOf(to));
                    }
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
                    q.setParameter("limit", limit);
                }, reservationColumns());
        return reservationRowsAnswer(rows, toolName, label, intro, "No encontré reservaciones que coincidan con tu pregunta.");
    }

    protected AiToolAnswer reservationList(String toolName, String label, String intro, LocalDate from, LocalDate to, String search, int limit) {
        return reservationList(toolName, label, intro, from, to, search, limit, false);
    }

    protected String reservationBaseSql(String whereClause, String orderBy, int limit) {
        return """
                SELECT r.id,
                       p.name AS property_name,
                       COALESCE(pl.name, '') AS platform_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.reservation_value,
                       r.status,
                       COALESCE(STRING_AGG(g.full_name, ', ' ORDER BY rg.is_primary DESC, g.full_name), '') AS guests,
                       COALESCE(COUNT(g.id), 0) AS guest_count
                FROM reservations r
                JOIN properties p ON p.id = r.property_id
                LEFT JOIN platforms pl ON pl.id = r.platform_id
                LEFT JOIN reservation_guests rg ON rg.reservation_id = r.id AND rg.organization_id = r.organization_id
                LEFT JOIN guests g ON g.id = rg.guest_id AND g.organization_id = r.organization_id AND g.deleted_at IS NULL
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                """
                + "  AND " + whereClause + System.lineSeparator()
                + "GROUP BY r.id, p.name, pl.name, r.reservation_code, r.check_in, r.check_out, r.reservation_value, r.status" + System.lineSeparator()
                + "ORDER BY " + orderBy + System.lineSeparator()
                + "LIMIT :limit" + System.lineSeparator();
    }

    protected void appendOptionalReservationDateFilters(StringBuilder sql, LocalDate[] range) {
        if (range[0] != null) {
            sql.append(" AND r.check_in >= :fromDate\n");
        }
        if (range[1] != null) {
            sql.append(" AND r.check_in <= :toDate\n");
        }
    }

    protected void setOptionalReservationDateParameters(Query query, LocalDate[] range) {
        if (range[0] != null) {
            query.setParameter("fromDate", Date.valueOf(range[0]));
        }
        if (range[1] != null) {
            query.setParameter("toDate", Date.valueOf(range[1]));
        }
    }

    protected String[] reservationColumns() {
        return new String[]{"id", "propertyName", "platformName", "reservationCode", "checkIn", "checkOut", "reservationValue", "status", "guests", "guestCount"};
    }

    protected AiToolAnswer reservationRowsAnswer(List<Map<String, Object>> rows, String toolName, String label, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, label, "No reservation rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | valor: ").append(formatMoney(row.get("reservationValue")));
            String platform = value(row.get("platformName"));
            if (!platform.isBlank()) {
                answer.append(" | plataforma: ").append(platform);
            }
            String guests = value(row.get("guests"));
            if (!guests.isBlank()) {
                answer.append(" | huéspedes: ").append(guests);
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d reservation rows found.".formatted(rows.size()), rows);
    }













    protected AiToolAnswer guestList(String toolName, String label, String intro, String search, boolean upcomingOnly, boolean recentOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
                SELECT g.id,
                       g.full_name,
                       g.status,
                       COUNT(DISTINCT rg.reservation_id) AS reservation_count,
                       MAX(r.check_in) AS last_check_in
                FROM guests g
                LEFT JOIN reservation_guests rg ON rg.guest_id = g.id AND rg.organization_id = g.organization_id
                LEFT JOIN reservations r ON r.id = rg.reservation_id AND r.organization_id = g.organization_id AND r.deleted_at IS NULL
                WHERE g.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                """);
        if (normalizedSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\s+') token(value)
                          WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, g.notes, g.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (upcomingOnly) {
            sql.append("  AND r.check_in >= CURRENT_DATE\n");
        }
        sql.append("""
                GROUP BY g.id, g.full_name, g.status
                ORDER BY MAX(r.check_in) DESC NULLS LAST, g.full_name ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "fullName", "status", "reservationCount", "lastCheckIn");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré huéspedes que coincidan con tu pregunta.", toolName, label, "No guests found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | reservaciones: ").append(blankToDash(value(row.get("reservationCount"))))
                    .append(" | última llegada: ").append(blankToDash(value(row.get("lastCheckIn"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d guest rows found.".formatted(rows.size()), rows);
    }

    protected AiToolAnswer guestReservationList(String toolName, String label, String intro, String search, boolean upcomingOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = nullableSearch(search);
        StringBuilder sql = new StringBuilder("""
                SELECT g.id,
                       g.full_name,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       rg.is_primary
                FROM reservation_guests rg
                JOIN guests g ON g.id = rg.guest_id
                JOIN reservations r ON r.id = rg.reservation_id
                JOIN properties p ON p.id = r.property_id
                WHERE rg.organization_id = :organizationId
                  AND g.organization_id = :organizationId
                  AND r.organization_id = :organizationId
                  AND g.deleted_at IS NULL
                  AND r.deleted_at IS NULL
                """);
        if (upcomingOnly) {
            sql.append("  AND r.check_in >= CURRENT_DATE\n");
        }
        if (normalizedSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\s+') token(value)
                          WHERE translate(LOWER(CONCAT_WS(' ', g.full_name, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append("""
                ORDER BY r.check_in DESC, rg.is_primary DESC, g.full_name ASC
                LIMIT :limit
                """);
        List<Map<String, Object>> rows = query(sql.toString(), q -> {
                    q.setParameter("organizationId", organizationId);
                    if (normalizedSearch != null) {
                        q.setParameter("search", normalizedSearch);
                    }
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "fullName", "propertyName", "reservationCode", "checkIn", "checkOut", "isPrimary");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré huéspedes asociados a reservaciones que coincidan con tu pregunta.", toolName, label, "No reservation guest rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | ").append(blankToDash(value(row.get("checkIn"))))
                    .append(" a ").append(blankToDash(value(row.get("checkOut"))));
            String code = value(row.get("reservationCode"));
            if (!code.isBlank()) {
                answer.append(" | código: ").append(code);
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, label, "%d reservation guest rows found.".formatted(rows.size()), rows);
    }

    protected String resolveScheduledMaintenanceStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "pausado", "pausados", "pausada", "pausadas", "paused")) {
            return "PAUSED";
        }
        if (containsAny(normalized, "completado", "completados", "completada", "completadas", "completed")) {
            return "COMPLETED";
        }
        if (containsAny(normalized, "cancelado", "cancelados", "cancelada", "canceladas", "cancelled", "canceled")) {
            return "CANCELLED";
        }
        return "ACTIVE";
    }

    protected String resolveReservationStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "cancelado", "cancelados", "cancelada", "canceladas", "cancelled", "canceled")) {
            return "CANCELLED";
        }
        if (containsAny(normalized, "eliminado", "eliminados", "deleted")) {
            return "DELETED";
        }
        return "ACTIVE";
    }

    protected LocalDate[] resolveDateRange(String userQuestion) {
        String normalized = normalize(userQuestion);
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "hoy")) {
            return new LocalDate[]{today, today};
        }
        if (containsAny(normalized, "semana")) {
            return new LocalDate[]{today, today.plusDays(7)};
        }
        if (containsAny(normalized, "mes")) {
            LocalDate start = today.withDayOfMonth(1);
            return new LocalDate[]{start, start.plusMonths(1).minusDays(1)};
        }
        return new LocalDate[]{null, null};
    }

    protected String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? blankToDash(second) : first;
    }









































































































    protected List<Map<String, Object>> documentRows(String search, String extraWhere, QueryConfigurer extraConfigurer, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT d.id,
                       d.title,
                       d.document_type,
                       d.processing_status,
                       d.status,
                       d.original_filename,
                       d.content_type,
                       d.size_bytes,
                       d.created_at,
                       COALESCE(p.name, '') AS property_name,
                       COUNT(dc.id) AS chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS indexed_chunk_count,
                       COALESCE(SUM(CASE WHEN dc.vector_store_id IS NULL AND dc.id IS NOT NULL THEN 1 ELSE 0 END), 0) AS missing_vector_id_count
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                """);
        if (extraWhere != null && !extraWhere.isBlank()) {
            sql.append(extraWhere).append(System.lineSeparator());
        }
        if (search != null && !search.isBlank()) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM regexp_split_to_table(CAST(:search AS TEXT), '\\s+') token(value)
                      WHERE translate(LOWER(CONCAT_WS(' ', d.title, d.description, d.original_filename, d.document_type, d.processing_status, d.status, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY d.id, d.title, d.document_type, d.processing_status, d.status, d.original_filename, d.content_type, d.size_bytes, d.created_at, p.name
                """);
        sql.append("ORDER BY ").append(orderBy).append(System.lineSeparator());
        sql.append("LIMIT :limit");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null && !search.isBlank()) {
                q.setParameter("search", search);
            }
            if (extraConfigurer != null) {
                extraConfigurer.configure(q);
            }
            q.setParameter("limit", limit);
        }, "id", "title", "documentType", "processingStatus", "status", "originalFilename", "contentType", "sizeBytes", "createdAt", "propertyName", "chunkCount", "indexedChunkCount", "missingVectorIdCount");
    }

    protected AiToolAnswer documentRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro, String emptyMessage) {
        if (rows.isEmpty()) {
            return AiToolAnswer.of(emptyMessage, toolName, "Document metadata", "No document rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder(intro);
        appendDocumentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), toolName, "Document metadata", "%d document rows found.".formatted(rows.size()), rows);
    }

    protected void appendDocumentRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("documentType"))))
                    .append(" | procesamiento: ").append(blankToDash(value(row.get("processingStatus"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | chunks indexados: ").append(blankToDash(value(row.get("indexedChunkCount"))))
                    .append("/").append(blankToDash(value(row.get("chunkCount"))));
        }
    }

    protected String documentTypeFilterFromQuestion(String normalized) {
        if (containsAny(normalized, "plano", "planos", "blueprint")) {
            return "IN ('BLUEPRINT', 'ELECTRICAL_PLAN', 'PLUMBING_PLAN', 'DRAINAGE_PLAN')";
        }
        if (containsAny(normalized, "regla", "reglas", "house rule", "senales", "señales")) {
            return "IN ('HOUSE_RULES', 'BATHROOM_RULES', 'PROPERTY_SIGNS')";
        }
        if (containsAny(normalized, "manual", "manuales")) {
            return "= 'MANUAL'";
        }
        return null;
    }

    protected List<Map<String, Object>> reservationSupplyRows(String itemSearch, LocalDate fromDate, LocalDate toDate, String propertySearch, String reservationSearch, String status, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT rs.id,
                       rs.item_name_snapshot AS item_name,
                       rs.quantity,
                       COALESCE(rs.unit, '') AS unit,
                       COALESCE(rs.notes, '') AS notes,
                       p.name AS property_name,
                       r.reservation_code,
                       r.check_in,
                       r.check_out,
                       r.status
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                """);
        if (fromDate != null) {
            sql.append("  AND r.check_in >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND r.check_in <= :toDate\n");
        }
        if (status != null && !status.isBlank()) {
            sql.append("  AND r.status = :status\n");
        }
        if (itemSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:itemSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', rs.item_name_snapshot, rs.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', p.name, p.address, p.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (reservationSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:reservationSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', r.reservation_code, r.observations, p.name, r.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (fromDate != null) {
                q.setParameter("fromDate", Date.valueOf(fromDate));
            }
            if (toDate != null) {
                q.setParameter("toDate", Date.valueOf(toDate));
            }
            if (status != null && !status.isBlank()) {
                q.setParameter("status", status);
            }
            if (itemSearch != null) {
                q.setParameter("itemSearch", itemSearch);
            }
            if (propertySearch != null) {
                q.setParameter("propertySearch", propertySearch);
            }
            if (reservationSearch != null) {
                q.setParameter("reservationSearch", reservationSearch);
            }
            q.setParameter("limit", limit);
        }, "id", "itemName", "quantity", "unit", "notes", "propertyName", "reservationCode", "checkIn", "checkOut", "status");
    }

    protected List<Map<String, Object>> reservationSupplySummaryRows(LocalDate fromDate, LocalDate toDate, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT rs.item_name_snapshot AS item_name,
                       COUNT(*) AS usage_count,
                       COALESCE(SUM(rs.quantity), 0) AS total_quantity,
                       COALESCE(MAX(rs.unit), '') AS unit,
                       MAX(r.check_in) AS last_check_in,
                       COALESCE(STRING_AGG(DISTINCT p.name, ', ' ORDER BY p.name), '') AS properties
                FROM reservation_supplies rs
                JOIN reservations r ON r.id = rs.reservation_id AND r.organization_id = rs.organization_id
                JOIN properties p ON p.id = r.property_id AND p.organization_id = rs.organization_id
                JOIN inventory_items ii ON ii.id = rs.inventory_item_id AND ii.organization_id = rs.organization_id
                WHERE rs.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND ii.deleted_at IS NULL
                  AND ii.item_type = 'SUPPLY'
                """);
        if (fromDate != null) {
            sql.append("  AND r.check_in >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND r.check_in <= :toDate\n");
        }
        sql.append("""
                GROUP BY rs.item_name_snapshot
                ORDER BY usage_count DESC, total_quantity DESC, last_check_in DESC NULLS LAST, rs.item_name_snapshot ASC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (fromDate != null) {
                q.setParameter("fromDate", Date.valueOf(fromDate));
            }
            if (toDate != null) {
                q.setParameter("toDate", Date.valueOf(toDate));
            }
            q.setParameter("limit", limit);
        }, "itemName", "usageCount", "totalQuantity", "unit", "lastCheckIn", "properties");
    }

    protected AiToolAnswer reservationSupplyRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | reserva: ").append(blankToDash(value(row.get("reservationCode"))))
                    .append(" | check-in: ").append(blankToDash(value(row.get("checkIn"))));
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Reservation supply rows", "%d reservation supply rows found.".formatted(rows.size()), rows);
    }

    protected String dateFilterSql(String column, LocalDate fromDate, LocalDate toDate) {
        StringBuilder sql = new StringBuilder();
        if (fromDate != null) {
            sql.append("  AND ").append(column).append(" >= :fromDate\n");
        }
        if (toDate != null) {
            sql.append("  AND ").append(column).append(" <= :toDate\n");
        }
        return sql.toString();
    }

    protected void setDateRangeParams(Query query, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) {
            query.setParameter("fromDate", Date.valueOf(fromDate));
        }
        if (toDate != null) {
            query.setParameter("toDate", Date.valueOf(toDate));
        }
    }

    protected List<Map<String, Object>> taskListRows(String search, String propertySearch, String reservationSearch, LocalDate fromDueDate, LocalDate toDueDate, List<String> statuses, LocalDate exactDueDate, int limit, String orderBy) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT tl.id,
                       p.name AS property_name,
                       COALESCE(r.reservation_code, '') AS reservation_code,
                       r.check_in,
                       tl.title,
                       tl.creation_date,
                       tl.due_date,
                       tl.status,
                       COUNT(ti.id) AS total_items,
                       COALESCE(SUM(CASE WHEN ti.completed = TRUE THEN 1 ELSE 0 END), 0) AS completed_items,
                       COALESCE(SUM(CASE WHEN ti.completed = FALSE THEN 1 ELSE 0 END), 0) AS pending_items
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                LEFT JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = tl.organization_id AND r.deleted_at IS NULL
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', tl.title, tl.status, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', p.name, p.address, p.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (reservationSearch != null) {
            sql.append("""
                      AND tl.reservation_id IS NOT NULL
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:reservationSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', r.reservation_code, r.observations, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (fromDueDate != null) {
            sql.append("  AND tl.due_date >= :fromDueDate\n");
        }
        if (toDueDate != null) {
            sql.append("  AND tl.due_date <= :toDueDate\n");
        }
        if (exactDueDate != null) {
            sql.append("  AND tl.due_date = :exactDueDate\n");
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append("  AND tl.status IN (:statuses)\n");
        }
        sql.append(" GROUP BY tl.id, p.name, r.reservation_code, r.check_in, tl.title, tl.creation_date, tl.due_date, tl.status\n");
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            if (propertySearch != null) {
                q.setParameter("propertySearch", propertySearch);
            }
            if (reservationSearch != null) {
                q.setParameter("reservationSearch", reservationSearch);
            }
            if (fromDueDate != null) {
                q.setParameter("fromDueDate", Date.valueOf(fromDueDate));
            }
            if (toDueDate != null) {
                q.setParameter("toDueDate", Date.valueOf(toDueDate));
            }
            if (exactDueDate != null) {
                q.setParameter("exactDueDate", Date.valueOf(exactDueDate));
            }
            if (statuses != null && !statuses.isEmpty()) {
                q.setParameter("statuses", statuses);
            }
            q.setParameter("limit", limit);
        }, "id", "propertyName", "reservationCode", "checkIn", "title", "creationDate", "dueDate", "status", "totalItems", "completedItems", "pendingItems");
    }

    protected AiToolAnswer taskListRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | vence: ").append(blankToDash(value(row.get("dueDate"))))
                    .append(" | avance: ").append(blankToDash(value(row.get("completedItems"))))
                    .append("/").append(blankToDash(value(row.get("totalItems"))));
            if (!value(row.get("reservationCode")).isBlank()) {
                answer.append(" | reserva: ").append(blankToDash(value(row.get("reservationCode"))));
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Task list rows", "%d task list rows found.".formatted(rows.size()), rows);
    }

    protected List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy) {
        return taskItemRows(search, taskListSearch, completed, overdueBefore, limit, orderBy, false);
    }

    protected List<Map<String, Object>> taskItemRows(String search, String taskListSearch, Boolean completed, LocalDate overdueBefore, int limit, String orderBy, boolean highPriorityOnly) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT ti.id,
                       ti.task_name,
                       ti.responsible_person,
                       ti.completed,
                       ti.completion_date,
                       ti.sort_order,
                       tl.title AS task_list_title,
                       tl.due_date,
                       tl.status AS task_list_status,
                       p.name AS property_name,
                       COALESCE(r.reservation_code, '') AS reservation_code,
                       COALESCE((
                           SELECT g.full_name
                           FROM reservation_guests rg
                           JOIN guests g ON g.id = rg.guest_id
                                        AND g.organization_id = rg.organization_id
                                        AND g.deleted_at IS NULL
                           WHERE rg.reservation_id = r.id
                             AND rg.organization_id = ti.organization_id
                           ORDER BY rg.is_primary DESC, g.full_name ASC
                           LIMIT 1
                       ), '') AS primary_guest
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = ti.organization_id
                LEFT JOIN reservations r ON r.id = tl.reservation_id AND r.organization_id = ti.organization_id AND r.deleted_at IS NULL
                WHERE ti.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', ti.task_name, ti.responsible_person, tl.title, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (taskListSearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1
                          FROM unnest(string_to_array(CAST(:taskListSearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', tl.title, p.name, r.reservation_code)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (completed != null) {
            sql.append("  AND ti.completed = :completed\n");
        }
        if (overdueBefore != null) {
            sql.append("  AND tl.due_date <= :overdueBefore\n");
        }
        if (highPriorityOnly) {
            sql.append("  AND ti.sort_order IN (0, 1)\n");
        }
        sql.append(" ORDER BY ").append(orderBy).append("\n LIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) {
                q.setParameter("search", search);
            }
            if (taskListSearch != null) {
                q.setParameter("taskListSearch", taskListSearch);
            }
            if (completed != null) {
                q.setParameter("completed", completed);
            }
            if (overdueBefore != null) {
                q.setParameter("overdueBefore", Date.valueOf(overdueBefore));
            }
            q.setParameter("limit", limit);
        }, "id", "taskName", "responsiblePerson", "completed", "completionDate", "sortOrder", "taskListTitle", "dueDate", "taskListStatus", "propertyName", "reservationCode", "primaryGuest");
    }

    protected AiToolAnswer taskItemRowsAnswer(List<Map<String, Object>> rows, String toolName, String intro) {
        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("taskName"))))
                    .append(" | lista: ").append(blankToDash(value(row.get("taskListTitle"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | completada: ").append(blankToDash(value(row.get("completed"))))
                    .append(" | vence lista: ").append(blankToDash(value(row.get("dueDate"))));
            if (!value(row.get("responsiblePerson")).isBlank()) {
                answer.append(" | responsable: ").append(blankToDash(value(row.get("responsiblePerson"))));
            }
        }
        return AiToolAnswer.of(answer.toString(), toolName, "Task item rows", "%d task item rows found.".formatted(rows.size()), rows);
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

    protected List<Map<String, Object>> purchaseListRows(String search, List<String> statuses, PurchaseDateRange range, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT pl.id,
                       p.name AS property_name,
                       s.name AS supplier_name,
                       pl.purchase_date,
                       pl.status,
                       pl.notes,
                       COUNT(pi.id) AS item_count,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE THEN 1 ELSE 0 END), 0) AS purchased_item_count,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE THEN pi.estimated_price ELSE 0 END), 0) AS purchased_total_cost
                FROM purchase_lists pl
                LEFT JOIN properties p ON p.id = pl.property_id
                                      AND p.organization_id = pl.organization_id
                LEFT JOIN suppliers s ON s.id = pl.supplier_id
                                     AND s.organization_id = pl.organization_id
                                     AND s.deleted_at IS NULL
                LEFT JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                           AND pi.organization_id = pl.organization_id
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (statuses != null && !statuses.isEmpty()) {
            sql.append("  AND pl.status IN (:statuses)\n");
        }
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', p.name, s.name, pl.notes, pl.status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY pl.id, p.name, s.name, pl.purchase_date, pl.status, pl.notes
                ORDER BY pl.purchase_date DESC, pl.created_at DESC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (statuses != null && !statuses.isEmpty()) {
                q.setParameter("statuses", statuses);
            }
            if (range != null) {
                q.setParameter("fromDate", Date.valueOf(range.fromDate()));
                q.setParameter("toDate", Date.valueOf(range.toDate()));
            }
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "id", "propertyName", "supplierName", "purchaseDate", "status", "notes", "itemCount", "purchasedItemCount", "purchasedTotalCost");
    }

    protected List<Map<String, Object>> purchaseItemRows(String search, List<String> flags, PurchaseDateRange range, int limit) {
        boolean purchasedOnly = flags != null && flags.contains("purchasedOnly");
        StringBuilder sql = new StringBuilder("""
                SELECT pi.id,
                       pi.item_name_snapshot,
                       ii.item_type,
                       pi.quantity,
                       pi.unit,
                       pi.estimated_price,
                       pi.purchased,
                       pl.purchase_date,
                       pl.status AS purchase_list_status,
                       p.name AS property_name,
                       s.name AS supplier_name
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                                      AND pl.organization_id = pi.organization_id
                LEFT JOIN inventory_items ii ON ii.id = pi.inventory_item_id
                                            AND ii.organization_id = pi.organization_id
                LEFT JOIN properties p ON p.id = pl.property_id
                                      AND p.organization_id = pl.organization_id
                LEFT JOIN suppliers s ON s.id = pl.supplier_id
                                     AND s.organization_id = pl.organization_id
                                     AND s.deleted_at IS NULL
                WHERE pi.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (purchasedOnly) {
            sql.append("  AND pi.purchased = TRUE\n");
        }
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes, ii.name, ii.item_type, p.name, s.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                ORDER BY pl.purchase_date DESC, pi.created_at DESC
                LIMIT :limit
                """);
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (range != null) {
                q.setParameter("fromDate", Date.valueOf(range.fromDate()));
                q.setParameter("toDate", Date.valueOf(range.toDate()));
            }
            if (search != null) {
                q.setParameter("search", search);
            }
            q.setParameter("limit", limit);
        }, "id", "itemName", "itemType", "quantity", "unit", "estimatedPrice", "purchased", "purchaseDate", "purchaseListStatus", "propertyName", "supplierName");
    }

    protected String purchaseCostBaseSql(PurchaseDateRange range, String groupBy, String orderBy) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT pl.id) AS list_count,
                       COUNT(pi.id) AS purchased_item_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(AVG(pi.estimated_price), 0) AS avg_line_cost,
                       MIN(pl.purchase_date) AS first_purchase_date,
                       MAX(pl.purchase_date) AS last_purchase_date
                FROM purchase_lists pl
                JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                      AND pi.organization_id = pl.organization_id
                                      AND pi.purchased = TRUE
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """);
        if (range != null) {
            sql.append("  AND pl.purchase_date >= :fromDate\n");
            sql.append("  AND pl.purchase_date <= :toDate\n");
        }
        return sql.toString();
    }

    protected void setPurchaseCostCommonParams(Query query, PurchaseDateRange range) {
        query.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
        if (range != null) {
            query.setParameter("fromDate", Date.valueOf(range.fromDate()));
            query.setParameter("toDate", Date.valueOf(range.toDate()));
        }
    }

    protected String purchaseItemAggregateSql(String search, String orderMetric, String direction) {
        String safeMetric = switch (orderMetric) {
            case "purchase_count" -> "purchase_count";
            case "total_quantity" -> "total_quantity";
            case "item_name" -> "item_name_snapshot";
            default -> "purchase_count";
        };
        String safeDirection = "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        StringBuilder sql = new StringBuilder("""
                SELECT pi.item_name_snapshot,
                       COUNT(*) AS purchase_count,
                       COALESCE(SUM(pi.quantity), 0) AS total_quantity,
                       COALESCE(MAX(pi.unit), '') AS unit,
                       COALESCE(SUM(pi.estimated_price), 0) AS total_cost,
                       COALESCE(AVG(pi.estimated_price), 0) AS average_line_cost,
                       COALESCE(AVG(CASE WHEN pi.quantity IS NOT NULL AND pi.quantity > 0 THEN pi.estimated_price / pi.quantity ELSE pi.estimated_price END), 0) AS average_unit_cost,
                       MIN(pl.purchase_date) AS first_purchase_date,
                       MAX(pl.purchase_date) AS last_purchase_date
                FROM purchase_items pi
                JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
                                      AND pl.organization_id = pi.organization_id
                WHERE pi.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                  AND pi.purchased = TRUE
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', pi.item_name_snapshot, pi.notes)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("""
                GROUP BY pi.item_name_snapshot
                """);
        sql.append("ORDER BY ").append(safeMetric).append(' ').append(safeDirection);
        if (!"purchase_count".equals(safeMetric)) {
            sql.append(", purchase_count ").append(safeDirection);
        }
        if (!"total_quantity".equals(safeMetric)) {
            sql.append(", total_quantity ").append(safeDirection);
        }
        sql.append(", pi.item_name_snapshot ASC\n");
        sql.append("LIMIT :limit\n");
        return sql.toString();
    }

    protected PurchaseDateRange purchaseDateRange(String userQuestion) {
        String normalized = normalize(userQuestion);
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "este mes", "mes actual", "this month")) {
            LocalDate from = today.withDayOfMonth(1);
            LocalDate to = from.plusMonths(1).minusDays(1);
            return new PurchaseDateRange(from, to, "este mes");
        }
        if (containsAny(normalized, "mes pasado", "ultimo mes", "último mes", "last month")) {
            LocalDate from = today.minusMonths(1).withDayOfMonth(1);
            LocalDate to = from.plusMonths(1).minusDays(1);
            return new PurchaseDateRange(from, to, "el mes pasado");
        }
        if (containsAny(normalized, "esta semana", "semana actual", "this week")) {
            LocalDate from = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            return new PurchaseDateRange(from, from.plusDays(6), "esta semana");
        }
        if (containsAny(normalized, "hoy", "today")) {
            return new PurchaseDateRange(today, today, "hoy");
        }
        if (containsAny(normalized, "este ano", "este año", "year to date", "este anio")) {
            LocalDate from = today.withDayOfYear(1);
            return new PurchaseDateRange(from, today, "este año");
        }
        return null;
    }

    protected void appendPurchaseListRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("purchaseDate"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))))
                    .append(" | items: ").append(blankToDash(value(row.get("itemCount"))))
                    .append(" | comprados: ").append(blankToDash(value(row.get("purchasedItemCount"))))
                    .append(" | gasto comprado: ").append(formatMoney(row.get("purchasedTotalCost")));
        }
    }

    protected void appendPurchaseItemRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("itemName"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("purchaseDate"))))
                    .append(" | cantidad: ").append(blankToDash(value(row.get("quantity")))).append(" ").append(blankToDash(value(row.get("unit"))))
                    .append(" | precio: ").append(formatMoney(row.get("estimatedPrice")))
                    .append(" | comprado: ").append(blankToDash(value(row.get("purchased"))))
                    .append(" | proveedor: ").append(blankToDash(value(row.get("supplierName"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))));
        }
    }

    protected void appendDocumentGroups(StringBuilder answer, List<Map<String, Object>> rows, String groupKey) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String groupName = blankToDash(value(row.get(groupKey)));
            grouped.computeIfAbsent(groupName, ignored -> new ArrayList<>()).add(row);
        }
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            answer.append(System.lineSeparator())
                    .append(entry.getKey())
                    .append(" | documentos: ")
                    .append(entry.getValue().size());
            for (Map<String, Object> row : entry.getValue()) {
                answer.append(System.lineSeparator())
                        .append("- ")
                        .append(blankToDash(value(row.get("title"))));
            }
        }
    }



































    protected List<Map<String, Object>> fileMetadataRows(String search, String propertySearch, String sourceType, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM (
                    SELECT 'DOCUMENT' AS source_type,
                           d.title AS display_name,
                           d.original_filename,
                           d.content_type,
                           d.size_bytes,
                           d.status,
                           d.processing_status AS detail_status,
                           COALESCE(p.name, 'Sin propiedad') AS property_name,
                           d.created_at
                    FROM documents d
                    LEFT JOIN properties p ON p.id = d.property_id
                                      AND p.organization_id = d.organization_id
                                      AND p.deleted_at IS NULL
                    WHERE d.organization_id = :organizationId
                        UNION ALL
                    SELECT 'PROPERTY_IMAGE' AS source_type,
                           pi.original_filename AS display_name,
                           pi.original_filename,
                           pi.content_type,
                           pi.size_bytes,
                           pi.status,
                           CASE WHEN pi.is_cover = TRUE THEN 'COVER' ELSE 'IMAGE' END AS detail_status,
                           p.name AS property_name,
                           pi.created_at
                    FROM property_images pi
                    JOIN properties p ON p.id = pi.property_id
                                     AND p.organization_id = pi.organization_id
                                     AND p.deleted_at IS NULL
                    WHERE pi.organization_id = :organizationId
                          AND pi.status = 'ACTIVE'
                    UNION ALL
                    SELECT 'MAINTENANCE_IMAGE' AS source_type,
                           mri.original_filename AS display_name,
                           mri.original_filename,
                           mri.content_type,
                           mri.size_bytes,
                           mri.status,
                           mr.status AS detail_status,
                           p.name AS property_name,
                           mri.created_at
                    FROM maintenance_record_images mri
                    JOIN maintenance_records mr ON mr.id = mri.maintenance_record_id
                                               AND mr.organization_id = mri.organization_id
                                               AND mr.deleted_at IS NULL
                    JOIN properties p ON p.id = mr.property_id
                                     AND p.organization_id = mr.organization_id
                                     AND p.deleted_at IS NULL
                    WHERE mri.organization_id = :organizationId
                          AND mri.status = 'ACTIVE'
                ) files
                WHERE 1 = 1
                """);
        if (sourceType != null) {
            sql.append("  AND source_type = :sourceType\n");
        }
        if (search != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(CONCAT_WS(' ', display_name, original_filename, content_type, detail_status)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        if (propertySearch != null) {
            sql.append("""
                      AND NOT EXISTS (
                          SELECT 1 FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                          WHERE token.value <> ''
                            AND translate(LOWER(property_name), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                      )
                    """);
        }
        sql.append("ORDER BY created_at DESC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (sourceType != null) q.setParameter("sourceType", sourceType);
            if (search != null) q.setParameter("search", search);
            if (propertySearch != null) q.setParameter("propertySearch", propertySearch);
            q.setParameter("limit", limit);
        }, "sourceType", "displayName", "originalFilename", "contentType", "sizeBytes", "status", "detailStatus", "propertyName", "createdAt");
    }

    protected List<Map<String, Object>> maintenanceImageRows(String search, boolean withoutImages, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT mr.id,
                       mr.title,
                       p.name AS property_name,
                       COALESCE(mr.performed_at, mr.scheduled_at) AS maintenance_date,
                       COUNT(mri.id) AS image_count,
                       COALESCE(STRING_AGG(mri.original_filename, ', ' ORDER BY mri.created_at DESC), '') AS filenames
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                                 AND p.organization_id = mr.organization_id
                LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                                                       AND mri.organization_id = mr.organization_id
                                                                                            AND mri.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', mr.title, mr.description, p.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("GROUP BY mr.id, mr.title, p.name, mr.performed_at, mr.scheduled_at\n");
        sql.append(withoutImages ? "HAVING COUNT(mri.id) = 0\n" : "HAVING COUNT(mri.id) > 0\n");
        sql.append("ORDER BY COALESCE(mr.performed_at, mr.scheduled_at) DESC NULLS LAST, mr.title ASC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "title", "propertyName", "maintenanceDate", "imageCount", "filenames");
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

    protected boolean isCurrentUserAdministrator() {
        String currentRole = currentUserService.getCurrentRole();
        return "ADMINISTRATOR".equals(currentRole) || "SUPER_ADMIN".equals(currentRole);
    }

    protected boolean isCurrentUserSuperAdmin() {
        return "SUPER_ADMIN".equals(currentUserService.getCurrentRole());
    }

    protected AiToolAnswer adminOnlyDenied(String toolName, String displayName) {
        String answer = "Esta consulta solo está disponible para usuarios con rol ADMINISTRATOR o SUPER_ADMIN. "
                + "Por seguridad, no puedo listar usuarios, roles ni accesos si tu sesión no tiene uno de esos roles.";

        return AiToolAnswer.of(
                answer,
                toolName,
                displayName,
                "Admin-only AI tool blocked for the current user.",
                List.of()
        );
    }

    protected List<Map<String, Object>> currentUserAccessRows() {
        UUID userId = currentUserService.getCurrentUserId();
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        List<Map<String, Object>> rows = query("""
            SELECT u.id,
                   TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS full_name,
                   u.email,
                   u.status AS user_status,
                   u.last_login_at,
                   u.password_change_required,
                   uo.status AS membership_status,
                   r.code AS role_code,
                   r.name AS role_name
            FROM user_organizations uo
            JOIN users u ON u.id = uo.user_id
            JOIN roles r ON r.id = uo.role_id
            WHERE uo.user_id = :userId
              AND uo.organization_id = :organizationId
              AND u.deleted_at IS NULL
            LIMIT 1
            """, q -> {
                    q.setParameter("userId", userId);
                    q.setParameter("organizationId", organizationId);
                },
                "id", "fullName", "email", "userStatus", "lastLoginAt", "passwordChangeRequired", "membershipStatus", "roleCode", "roleName");

        if (!rows.isEmpty() || !isCurrentUserSuperAdmin()) {
            return rows;
        }

        return query("""
            SELECT u.id,
                   TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS full_name,
                   u.email,
                   u.status AS user_status,
                   u.last_login_at,
                   u.password_change_required,
                   CAST('GLOBAL_SUPER_ADMIN' AS TEXT) AS membership_status,
                   CAST('SUPER_ADMIN' AS TEXT) AS role_code,
                   CAST('Super Admin' AS TEXT) AS role_name
            FROM users u
            WHERE u.id = :userId
              AND u.deleted_at IS NULL
            LIMIT 1
            """, q -> q.setParameter("userId", userId),
                "id", "fullName", "email", "userStatus", "lastLoginAt", "passwordChangeRequired", "membershipStatus", "roleCode", "roleName");
    }

    protected List<Map<String, Object>> userRows(String statusFilterSql, String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT u.id,
                       TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS full_name,
                       u.email,
                       u.status AS user_status,
                       u.last_login_at,
                       u.password_change_required,
                       uo.status AS membership_status,
                       r.code AS role_code,
                       r.name AS role_name
                FROM user_organizations uo
                JOIN users u ON u.id = uo.user_id
                JOIN roles r ON r.id = uo.role_id
                WHERE uo.organization_id = :organizationId
                  AND u.deleted_at IS NULL
                """);
        if (statusFilterSql != null && !statusFilterSql.isBlank()) {
            sql.append("  AND ").append(statusFilterSql).append(System.lineSeparator());
        }
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', u.first_name, u.last_name, u.email, u.status, uo.status, r.code, r.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("ORDER BY r.code ASC, u.first_name ASC, u.last_name ASC, u.email ASC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "fullName", "email", "userStatus", "lastLoginAt", "passwordChangeRequired", "membershipStatus", "roleCode", "roleName");
    }

    protected List<Map<String, Object>> userRowsByRole(String search, int limit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        StringBuilder sql = new StringBuilder("""
                SELECT u.id,
                       TRIM(CONCAT(u.first_name, ' ', u.last_name)) AS full_name,
                       u.email,
                       u.status AS user_status,
                       uo.status AS membership_status,
                       r.code AS role_code,
                       r.name AS role_name
                FROM user_organizations uo
                JOIN users u ON u.id = uo.user_id
                JOIN roles r ON r.id = uo.role_id
                WHERE uo.organization_id = :organizationId
                  AND u.deleted_at IS NULL
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', r.code, r.name, r.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("ORDER BY r.code ASC, u.first_name ASC, u.last_name ASC, u.email ASC\nLIMIT :limit\n");
        return query(sql.toString(), q -> {
            q.setParameter("organizationId", organizationId);
            if (search != null) q.setParameter("search", search);
            q.setParameter("limit", limit);
        }, "id", "fullName", "email", "userStatus", "membershipStatus", "roleCode", "roleName");
    }

    protected List<Map<String, Object>> roleRows(String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.code, r.name, r.description
                FROM roles r
                WHERE 1 = 1
                """);
        if (search != null) {
            sql.append("""
                  AND NOT EXISTS (
                      SELECT 1 FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                      WHERE token.value <> ''
                        AND translate(LOWER(CONCAT_WS(' ', r.code, r.name, r.description)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                  )
                """);
        }
        sql.append("ORDER BY r.code ASC\n");
        return query(sql.toString(), q -> {
            if (search != null) q.setParameter("search", search);
        }, "id", "code", "name", "description");
    }

    protected void appendUserRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | correo: ").append(blankToDash(value(row.get("email"))))
                    .append(" | rol: ").append(blankToDash(value(row.get("roleCode"))));
        }
    }

    protected String rolePermissionText(String code, String description) {
        String normalizedCode = value(code).toUpperCase(Locale.ROOT);
        return switch (normalizedCode) {
            case "SUPER_ADMIN" -> "acceso global de administración: puede administrar organizaciones, navegar entre organizaciones activas y hereda permisos operativos dentro de la organización seleccionada. "
                    + blankToDash(description);
            case "ADMINISTRATOR" -> "acceso completo dentro de la organización. " + blankToDash(description);
            case "PROPERTY_MANAGER" -> "gestiona la operación diaria de propiedades. " + blankToDash(description);
            case "MAINTENANCE_STAFF" -> "apoya con mantenimiento y tareas asignadas. " + blankToDash(description);
            case "READ_ONLY" -> "consulta información sin modificar datos. " + blankToDash(description);
            default -> blankToDash(description);
        };
    }

    protected interface AlertRowFormatter {
        String format(Map<String, Object> row);
    }

    protected void appendAlertGroup(StringBuilder answer, String title, List<Map<String, Object>> rows, AlertRowFormatter formatter) {
        answer.append(System.lineSeparator())
                .append("- ").append(title).append(": ").append(rows.size());
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("  - ").append(formatter.format(row));
        }
    }

    protected void appendFileMetadataRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("displayName"))))
                    .append(" | origen: ").append(blankToDash(value(row.get("sourceType"))))
                    .append(" | archivo: ").append(blankToDash(value(row.get("originalFilename"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | tipo: ").append(blankToDash(value(row.get("contentType"))))
                    .append(" | tamaño: ").append(formatBytes(toLong(row.get("sizeBytes"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))));
        }
    }

    protected void appendMaintenanceImageRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | fecha: ").append(formatDateTime(row.get("maintenanceDate")))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
            String filenames = value(row.get("filenames"));
            if (!filenames.isBlank()) {
                for (String filename : splitCommaValues(filenames)) {
                    answer.append(System.lineSeparator())
                            .append("	- ").append(filename);
                }
            }
        }
    }

    protected record PurchaseDateRange(LocalDate fromDate, LocalDate toDate, String label) {
    }

    protected Object scalar(String sql, QueryConfigurer configurer) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        return normalizeValue(query.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> query(String sql, QueryConfigurer configurer, String... columns) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        List<Object> resultList = query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object result : resultList) {
            Object[] values = result instanceof Object[] array ? array : new Object[]{result};
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < columns.length; index++) {
                Object value = index < values.length ? values[index] : null;
                row.put(columns[index], normalizeValue(value));
            }
            rows.add(row);
        }
        return rows;
    }

    protected Object normalizeValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        return value;
    }

    protected String extractSearchText(String userQuestion, String... extraStopWords) {
        if (userQuestion == null) {
            return "";
        }
        Set<String> extra = Arrays.stream(extraStopWords)
                .map(this::normalize)
                .collect(Collectors.toSet());
        String cleaned = keepSearchCharacters(normalize(userQuestion));
        return trimSearch(splitWords(cleaned).stream()
                .filter(word -> !SEARCH_STOP_WORDS.contains(word))
                .filter(word -> !extra.contains(word))
                .collect(Collectors.joining(" ")));
    }

    protected String trimSearch(String value) {
        String cleaned = collapseWhitespace(value);
        return cleaned.length() > 60 ? cleaned.substring(0, 60).trim() : cleaned;
    }

    protected String nullableSearch(String search) {
        return search == null || search.isBlank() ? null : search;
    }

    protected boolean containsAny(String value, String... candidates) {
        return AiToolTextNormalizer.containsAny(value, candidates);
    }

    protected String normalize(String value) {
        return AiToolTextNormalizer.normalize(value);
    }

    protected String keepSearchCharacters(String value) {
        return AiToolTextNormalizer.keepSearchCharacters(value);
    }

    protected String collapseWhitespace(String value) {
        return AiToolTextNormalizer.collapseWhitespace(value);
    }

    protected List<String> splitWords(String value) {
        return AiToolTextNormalizer.splitWords(value);
    }

    protected List<String> splitLines(String value) {
        return AiToolTextNormalizer.splitLines(value);
    }

    protected String value(Object value) {
        return value == null ? "" : value.toString();
    }

    protected String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    protected String formatBooleanYesNo(Object value) {
        String text = normalize(value(value));
        if (text.isBlank()) {
            return "—";
        }
        return containsAny(text, "true", "t", "yes", "si", "sí", "1") ? "Sí" : "No";
    }

    protected String formatDateTime(Object value) {
        String text = value(value).trim();
        if (text.isBlank()) {
            return "—";
        }

        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            String date = text.substring(0, tIndex);
            String rest = text.substring(tIndex + 1);
            StringBuilder time = new StringBuilder();
            for (int i = 0; i < rest.length() && time.length() < 8; i++) {
                char current = rest.charAt(i);
                if ((current >= '0' && current <= '9') || current == ':') {
                    time.append(current);
                } else {
                    break;
                }
            }
            return time.isEmpty() ? date : date + " " + time;
        }

        if (text.length() >= 19 && text.charAt(10) == ' ') {
            return text.substring(0, 19);
        }
        return text;
    }

    protected String formatDateTimeMinutes(Object value) {
        String formatted = formatDateTime(value);
        if (formatted.length() >= 16 && formatted.charAt(10) == ' ') {
            return formatted.substring(0, 16);
        }
        return formatted;
    }

    protected List<String> splitCommaValues(String value) {
        List<String> values = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ',') {
                String item = current.toString().trim();
                if (!item.isBlank()) {
                    values.add(item);
                }
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        String item = current.toString().trim();
        if (!item.isBlank()) {
            values.add(item);
        }
        return values;
    }

    protected String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> lines = splitLines(value);
        return lines.isEmpty() ? value.trim() : lines.get(0).trim();
    }

    protected String formatTimelineContent(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        List<String> lines = splitLines(value);
        if (lines.isEmpty()) {
            return value.trim();
        }
        StringBuilder builder = new StringBuilder(lines.get(0).trim());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isBlank()) {
                builder.append(System.lineSeparator()).append("	").append(line);
            }
        }
        return builder.toString();
    }

    protected String formatMoney(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "—";
        }
        return "Q " + value;
    }


    protected String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.ROOT, "%.1f GB", gb);
    }

    protected String joinName(Object firstName, Object lastName) {
        return (value(firstName) + " " + value(lastName)).trim();
    }


    protected long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value(value);
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    @FunctionalInterface
    protected interface QueryConfigurer {
        void configure(Query query);
    }

}
