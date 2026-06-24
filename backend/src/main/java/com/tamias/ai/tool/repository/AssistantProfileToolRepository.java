package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class AssistantProfileToolRepository extends AiReadOnlyToolSupport {

    public AssistantProfileToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer capabilities() {
        String answer = """
                Soy el asistente IA de TAMIAS.
                Te ayudo a consultar información operativa de tus alojamientos sin modificar datos. Puedo apoyarte con propiedades, catálogos, reservaciones próximas, mantenimientos, compras, pagos, tareas, documentos, modelos 3D Product Box y estado del índice RAG. También puedo combinar varias consultas para darte una visión operativa más útil. Por seguridad, en esta fase sigo siendo read-only: no creo, edito, elimino registros ni envío notificaciones automáticamente.
                """.trim();

        return AiToolAnswer.of(
                answer,
                "assistant.capabilities",
                "Assistant capabilities",
                "Static TAMIAS assistant capabilities response.",
                List.of()
        );
    }

    public AiToolAnswer currentUserProfile(String userQuestion) {
        UUID userId = currentUserService.getCurrentUserId();
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT u.first_name,
                       u.last_name,
                       u.email,
                       u.status,
                       u.password_change_required,
                       r.code AS role_code,
                       o.name AS organization_name
                FROM users u
                JOIN user_organizations uo ON uo.user_id = u.id
                JOIN organizations o ON o.id = uo.organization_id
                JOIN roles r ON r.id = uo.role_id
                WHERE u.id = :userId
                  AND uo.organization_id = :organizationId
                  AND u.deleted_at IS NULL
                  AND o.deleted_at IS NULL
                LIMIT 1
                """, q -> {
                    q.setParameter("userId", userId);
                    q.setParameter("organizationId", organizationId);
                }, "firstName", "lastName", "email", "status", "passwordChangeRequired", "role", "organizationName");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré información del usuario autenticado en TAMIAS.",
                    "user.currentProfile",
                    "Current user profile",
                    "No profile data found.",
                    List.of()
            );
        }

        Map<String, Object> profile = rows.get(0);
        String fullName = joinName(profile.get("firstName"), profile.get("lastName"));
        String email = value(profile.get("email"));
        String role = value(profile.get("role"));
        String organizationName = value(profile.get("organizationName"));
        String normalizedQuestion = normalize(userQuestion);

        String answer;
        if (containsAny(normalizedQuestion, "telefono", "numero de telefono", "celular")) {
            answer = "No veo un número de teléfono registrado en tu perfil de TAMIAS.";
        } else if (containsAny(normalizedQuestion, "correo", "email")) {
            answer = "El correo actual que estás usando es " + blankToDash(email) + ".";
        } else if (containsAny(normalizedQuestion, "usuario")) {
            answer = "Estás usando el usuario " + blankToDash(email) + ".";
        } else if (containsAny(normalizedQuestion, "rol")) {
            answer = "Tu rol actual en TAMIAS es " + blankToDash(role) + ".";
        } else if (containsAny(normalizedQuestion, "organizacion")) {
            answer = "Estás trabajando en la organización " + blankToDash(organizationName) + ".";
        } else if (containsAny(normalizedQuestion, "nombre", "llamo")) {
            answer = "Te llamas " + blankToDash(fullName) + ".";
        } else {
            answer = "Tienes sesión activa como " + blankToDash(fullName)
                    + " (" + blankToDash(email) + "), con rol " + blankToDash(role)
                    + " dentro de " + blankToDash(organizationName) + ".";
        }

        return AiToolAnswer.of(
                answer,
                "user.currentProfile",
                "Current user profile",
                "Current authenticated user profile was consulted.",
                rows
        );
    }

    public AiToolAnswer currentOrganizationSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT o.name,
                       o.description,
                       o.status,
                       COUNT(DISTINCT uo.user_id) AS user_count
                FROM organizations o
                LEFT JOIN user_organizations uo ON uo.organization_id = o.id
                    AND uo.status = 'ACTIVE'
                WHERE o.id = :organizationId
                  AND o.deleted_at IS NULL
                GROUP BY o.id, o.name, o.description, o.status
                LIMIT 1
                """, q -> q.setParameter("organizationId", organizationId), "name", "description", "status", "userCount");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré la organización actual asociada a tu usuario.",
                    "organization.currentSummary",
                    "Current organization",
                    "No organization found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = "Estás trabajando en " + blankToDash(value(row.get("name"))) + ".\n"
                + "Estado: " + blankToDash(value(row.get("status"))) + ".\n"
                + "Usuarios activos asociados: " + blankToDash(value(row.get("userCount"))) + ".";

        return AiToolAnswer.of(
                answer,
                "organization.currentSummary",
                "Current organization",
                "Current organization summary was consulted.",
                rows
        );
    }
}
