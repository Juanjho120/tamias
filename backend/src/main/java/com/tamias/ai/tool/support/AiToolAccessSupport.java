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

public abstract class AiToolAccessSupport extends AiChatHistoryReadSupport {

    protected AiToolAccessSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
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
}
