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
public class UserRoleOrganizationToolRepository extends AiReadOnlyToolSupport {

    public UserRoleOrganizationToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer activeUsers() {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("user.activeUsers", "Active users");
        }
        List<Map<String, Object>> rows = userRows("u.status = 'ACTIVE' AND uo.status = 'ACTIVE'", null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré usuarios activos en tu organización.",
                    "user.activeUsers",
                    "Active users",
                    "No active users were found for the current organization.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estos son los usuarios activos de tu organización:");
        appendUserRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "user.activeUsers", "Active users", "Active users for the current organization were consulted.", rows);
    }

    public AiToolAnswer inactiveUsers() {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("user.inactiveUsers", "Inactive users");
        }
        List<Map<String, Object>> rows = userRows("(u.status <> 'ACTIVE' OR uo.status <> 'ACTIVE')", null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré usuarios inactivos, bloqueados, invitados o desactivados en tu organización.",
                    "user.inactiveUsers",
                    "Inactive users",
                    "No inactive users were found for the current organization.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Estos son los usuarios no activos de tu organización:");
        appendUserRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "user.inactiveUsers", "Inactive users", "Inactive or non-active users for the current organization were consulted.", rows);
    }

    public AiToolAnswer searchUsers(String userQuestion) {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("user.search", "User search");
        }
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "usuario", "usuarios", "persona", "personas", "miembro", "miembros", "equipo", "organizacion", "organización"
        ));
        List<Map<String, Object>> rows = userRows(null, search, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré usuarios asociados a tu organización."
                            : "No encontré usuarios que coincidan con “" + search + "” dentro de tu organización.",
                    "user.search",
                    "User search",
                    "No users matched the search criteria.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(search == null
                ? "Estos son los usuarios asociados a tu organización:"
                : "Encontré estos usuarios relacionados con “" + search + "”:");
        appendUserRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "user.search", "User search", "Users for the current organization were consulted.", rows);
    }

    public AiToolAnswer usersByRole(String userQuestion) {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("user.byRole", "Users by role");
        }
        String normalizedQuestion = normalize(userQuestion);
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "usuario", "usuarios", "son", "con", "tiene", "tienen", "rol", "role", "roles", "administradores", "administrador", "activos", "inactivos"
        ));
        if (containsAny(normalizedQuestion, "administrador", "administradores", "administrator", "admin")) {
            search = "administrator";
        } else if (containsAny(normalizedQuestion, "property manager", "property managers", "manager")) {
            search = "property manager";
        } else if (containsAny(normalizedQuestion, "maintenance staff", "mantenimiento")) {
            search = "maintenance staff";
        } else if (containsAny(normalizedQuestion, "read only", "solo lectura", "lectura")) {
            search = "read only";
        }

        List<Map<String, Object>> rows = userRowsByRole(search, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré usuarios agrupados por rol en tu organización."
                            : "No encontré usuarios con un rol relacionado con “" + search + "” en tu organización.",
                    "user.byRole",
                    "Users by role",
                    "No users matched the requested role.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estos son los usuarios agrupados por rol:"
                : "Estos son los usuarios con rol relacionado con “" + search + "”:");
        String currentRole = "";
        for (Map<String, Object> row : rows) {
            String role = blankToDash(value(row.get("roleCode")));
            if (!role.equals(currentRole)) {
                currentRole = role;
                answer.append(System.lineSeparator()).append(role).append(":");
            }
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | correo: ").append(blankToDash(value(row.get("email"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("userStatus"))));
        }
        return AiToolAnswer.of(answer.toString(), "user.byRole", "Users by role", "Users by role for the current organization were consulted.", rows);
    }

    public AiToolAnswer userAccessSummary(String userQuestion) {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("user.accessSummary", "User access summary");
        }

        String normalizedQuestion = normalize(userQuestion);
        boolean wantsCurrentUserSummary = containsAny(
                normalizedQuestion,
                "este usuario", "mi usuario", "mis accesos", "mi acceso", "mi cuenta", "accesos tengo", "acceso tengo", "que accesos tengo", "qué accesos tengo"
        ) || (containsAny(normalizedQuestion, "acceso", "accesos", "permiso", "permisos")
                && containsAny(normalizedQuestion, "tengo", "mis", "mi"));
        boolean wantsOrganizationSummary = containsAny(
                normalizedQuestion,
                "todos", "todas", "todos los usuarios", "todas las personas", "usuarios", "equipo", "organizacion", "organización"
        ) && !wantsCurrentUserSummary;

        String search = wantsCurrentUserSummary || wantsOrganizationSummary
                ? null
                : nullableSearch(extractSearchText(
                    userQuestion,
                    "acceso", "accesos", "usuario", "usuarios", "este", "esta", "ese", "esa", "permisos", "tiene", "tienen", "tengo", "mis", "mi", "todos", "todas", "resumen", "rol", "roles"
                ));

        List<Map<String, Object>> rows;
        String intro;
        String evidenceSummary;
        if (wantsCurrentUserSummary || (search == null && !wantsOrganizationSummary)) {
            rows = currentUserAccessRows();
            intro = "Este es tu acceso actual en TAMIAS:";
            evidenceSummary = "Current authenticated user access metadata was consulted.";
        } else {
            rows = userRows(null, search, DEFAULT_LIMIT);
            intro = search == null
                    ? "Resumen de accesos de usuarios en tu organización:"
                    : "Resumen de accesos de usuarios relacionados con “" + search + "”:";
            evidenceSummary = "User access metadata for the current organization was consulted.";
        }

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré información de accesos para la consulta solicitada."
                            : "No encontré usuarios relacionados con “" + search + "” para resumir accesos.",
                    "user.accessSummary",
                    "User access summary",
                    "No users matched the access summary criteria.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(intro);
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("fullName"))))
                    .append(" | correo: ").append(blankToDash(value(row.get("email"))))
                    .append(" | rol: ").append(blankToDash(value(row.get("roleCode"))))
                    .append(" | usuario: ").append(blankToDash(value(row.get("userStatus"))))
                    .append(" | membresía: ").append(blankToDash(value(row.get("membershipStatus"))))
                    .append(" | último login: ").append(blankToDash(value(row.get("lastLoginAt"))))
                    .append(" | cambio de contraseña requerido: ").append(blankToDash(value(row.get("passwordChangeRequired"))));
        }
        answer.append(System.lineSeparator())
                .append("No muestro contraseñas, hashes, tokens ni información interna de seguridad.");
        return AiToolAnswer.of(answer.toString(), "user.accessSummary", "User access summary", evidenceSummary, rows);
    }

    public AiToolAnswer roleList() {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("role.list", "Role list");
        }
        List<Map<String, Object>> rows = roleRows(null);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré roles configurados en TAMIAS.", "role.list", "Role list", "No roles found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son los roles configurados en TAMIAS:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("code"))))
                    .append(" — ").append(blankToDash(value(row.get("name"))))
                    .append(" | ").append(blankToDash(value(row.get("description"))));
        }
        return AiToolAnswer.of(answer.toString(), "role.list", "Role list", "Configured TAMIAS roles were consulted.", rows);
    }

    public AiToolAnswer rolePermissionSummary(String userQuestion) {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("role.permissionSummary", "Role permission summary");
        }
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "permiso", "permisos", "tiene", "rol", "role", "resumen", "que", "cuales", "cuáles"
        ));
        List<Map<String, Object>> rows = roleRows(search);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré roles configurados para resumir permisos."
                            : "No encontré un rol relacionado con “" + search + "”.",
                    "role.permissionSummary",
                    "Role permission summary",
                    "No role matched the permission summary criteria.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder("Resumen de permisos por rol:");
        for (Map<String, Object> row : rows) {
            String code = value(row.get("code"));
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(code))
                    .append(" — ").append(blankToDash(value(row.get("name"))))
                    .append(": ").append(rolePermissionText(code, value(row.get("description"))));
        }
        answer.append(System.lineSeparator())
                .append("Nota: el esquema actual tiene roles organizacionales, pero no una tabla separada de permisos granulares. Por eso resumo el alcance según el rol configurado.");
        return AiToolAnswer.of(answer.toString(), "role.permissionSummary", "Role permission summary", "Role permission summary was generated from configured roles.", rows);
    }

    public AiToolAnswer organizationUserCount() {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("organization.userCount", "Organization user count");
        }
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT u.id) AS total_users,
                       COUNT(DISTINCT CASE WHEN u.status = 'ACTIVE' AND uo.status = 'ACTIVE' THEN u.id END) AS active_users,
                       COUNT(DISTINCT CASE WHEN u.status <> 'ACTIVE' OR uo.status <> 'ACTIVE' THEN u.id END) AS non_active_users,
                       COUNT(DISTINCT CASE WHEN r.code = 'ADMINISTRATOR' AND u.status = 'ACTIVE' AND uo.status = 'ACTIVE' THEN u.id END) AS administrators,
                       COUNT(DISTINCT CASE WHEN r.code = 'PROPERTY_MANAGER' AND u.status = 'ACTIVE' AND uo.status = 'ACTIVE' THEN u.id END) AS property_managers,
                       COUNT(DISTINCT CASE WHEN r.code = 'MAINTENANCE_STAFF' AND u.status = 'ACTIVE' AND uo.status = 'ACTIVE' THEN u.id END) AS maintenance_staff,
                       COUNT(DISTINCT CASE WHEN r.code = 'READ_ONLY' AND u.status = 'ACTIVE' AND uo.status = 'ACTIVE' THEN u.id END) AS read_only_users
                FROM user_organizations uo
                JOIN users u ON u.id = uo.user_id
                JOIN roles r ON r.id = uo.role_id
                WHERE uo.organization_id = :organizationId
                  AND u.deleted_at IS NULL
                """, q -> q.setParameter("organizationId", organizationId),
                "totalUsers", "activeUsers", "nonActiveUsers", "administrators", "propertyManagers", "maintenanceStaff", "readOnlyUsers");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Usuarios de tu organización:\n"
                + "- Total: " + blankToDash(value(row.get("totalUsers"))) + "\n"
                + "- Activos: " + blankToDash(value(row.get("activeUsers"))) + "\n"
                + "- No activos: " + blankToDash(value(row.get("nonActiveUsers"))) + "\n"
                + "- Administradores activos: " + blankToDash(value(row.get("administrators"))) + "\n"
                + "- Property Managers activos: " + blankToDash(value(row.get("propertyManagers"))) + "\n"
                + "- Maintenance Staff activos: " + blankToDash(value(row.get("maintenanceStaff"))) + "\n"
                + "- Read Only activos: " + blankToDash(value(row.get("readOnlyUsers")));
        return AiToolAnswer.of(answer, "organization.userCount", "Organization user count", "Organization user counts were consulted.", rows);
    }

    public AiToolAnswer organizationModuleUsageSummary() {
        if (!isCurrentUserAdministrator()) {
            return adminOnlyDenied("organization.moduleUsageSummary", "Organization module usage summary");
        }
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT
                  (SELECT COUNT(*) FROM properties p WHERE p.organization_id = :organizationId AND p.deleted_at IS NULL) AS properties_count,
                  (SELECT COUNT(*) FROM reservations r WHERE r.organization_id = :organizationId AND r.deleted_at IS NULL) AS reservations_count,
                  (SELECT COUNT(*) FROM maintenance_records mr WHERE mr.organization_id = :organizationId AND mr.deleted_at IS NULL) AS maintenance_records_count,
                  (SELECT COUNT(*) FROM scheduled_maintenance sm WHERE sm.organization_id = :organizationId AND sm.deleted_at IS NULL) AS scheduled_maintenance_count,
                  (SELECT COUNT(*) FROM purchase_lists pl WHERE pl.organization_id = :organizationId AND pl.deleted_at IS NULL) AS purchase_lists_count,
                  (SELECT COUNT(*) FROM task_lists tl WHERE tl.organization_id = :organizationId AND tl.deleted_at IS NULL) AS task_lists_count,
                  (SELECT COUNT(*) FROM documents d WHERE d.organization_id = :organizationId AND d.deleted_at IS NULL) AS documents_count,
                  (SELECT COUNT(*) FROM ai_chat_sessions acs WHERE acs.organization_id = :organizationId) AS ai_chat_sessions_count
                """, q -> q.setParameter("organizationId", organizationId),
                "properties", "reservations", "maintenanceRecords", "scheduledMaintenance", "purchaseLists", "taskLists", "documents", "aiChatSessions");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String answer = "Uso de módulos en tu organización:\n"
                + "- Propiedades: " + blankToDash(value(row.get("properties"))) + "\n"
                + "- Reservaciones: " + blankToDash(value(row.get("reservations"))) + "\n"
                + "- Mantenimientos: " + blankToDash(value(row.get("maintenanceRecords"))) + "\n"
                + "- Mantenimientos programados: " + blankToDash(value(row.get("scheduledMaintenance"))) + "\n"
                + "- Listas de compras: " + blankToDash(value(row.get("purchaseLists"))) + "\n"
                + "- Listas de tareas: " + blankToDash(value(row.get("taskLists"))) + "\n"
                + "- Documentos: " + blankToDash(value(row.get("documents"))) + "\n"
                + "- Sesiones del asistente IA: " + blankToDash(value(row.get("aiChatSessions")));
        return AiToolAnswer.of(answer, "organization.moduleUsageSummary", "Organization module usage summary", "Organization module usage counts were consulted.", rows);
    }
}
