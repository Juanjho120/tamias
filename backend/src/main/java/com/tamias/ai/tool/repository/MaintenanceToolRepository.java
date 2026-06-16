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
public class MaintenanceToolRepository extends AiReadOnlyToolSupport {

    public MaintenanceToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer lastPerformedMaintenance(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "ultimo", "ultima", "mantenimiento", "mantenimientos", "realizado", "realizados",
                "completado", "completados", "reciente", "recientes"
        ));
        List<Map<String, Object>> rows = query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.description,
                       mc.name AS category_name,
                       mt.name AS type_name,
                       mr.performed_at,
                       mr.scheduled_at,
                       mr.cost,
                       mr.status
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                LEFT JOIN maintenance_types mt ON mt.id = mr.maintenance_type_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.status = 'COMPLETED'
                  AND (
                       CAST(:search AS TEXT) IS NULL
                       OR LOWER(mr.title) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mr.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mc.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(COALESCE(mt.name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                       OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS TEXT), '%'))
                  )
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT 1
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("search", search);
                }, "id", "propertyName", "title", "description", "categoryName", "typeName", "performedAt", "scheduledAt", "cost", "status");

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré mantenimientos completados en tu organización."
                            : "No encontré mantenimientos completados relacionados con “" + search + "”.",
                    "maintenance.lastPerformed",
                    "Last performed maintenance",
                    "No matching maintenance record found.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.get(0);
        String answer = """
                El último mantenimiento completado que encontré fue:
                - Propiedad: %s
                - Título: %s
                - Categoría: %s
                - Tipo: %s
                - Fecha realizada: %s
                - Estado: %s
                - Costo: %s
                """.formatted(
                blankToDash(value(row.get("propertyName"))),
                blankToDash(value(row.get("title"))),
                blankToDash(value(row.get("categoryName"))),
                blankToDash(value(row.get("typeName"))),
                blankToDash(value(row.get("performedAt"))),
                blankToDash(value(row.get("status"))),
                formatMoney(row.get("cost"))
        ).trim();
        return AiToolAnswer.of(
                answer,
                "maintenance.lastPerformed",
                "Last performed maintenance",
                "Most recent matching completed maintenance record found.",
                rows
        );
    }

    public AiToolAnswer maintenanceSearch(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "buscar", "busca", "lista", "listar", "recientes", "reciente", "realizados", "realizado"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, search, null, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null ? "No encontré mantenimientos registrados." : "No encontré mantenimientos relacionados con “" + search + "”.",
                    "maintenance.search",
                    "Maintenance search",
                    "No maintenance records found.",
                    List.of()
            );
        }
        return maintenanceRowsAnswer(rows, "maintenance.search", "Maintenance search", search == null ? "Estos son los mantenimientos más recientes:" : "Encontré estos mantenimientos relacionados con “" + search + "”:");
    }

    public AiToolAnswer recentMaintenance() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos recientes.", "maintenance.recent", "Recent maintenance", "No recent maintenance records found.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.recent", "Recent maintenance", "Estos son los mantenimientos más recientes:");
    }

    public AiToolAnswer maintenanceByStatus(String userQuestion) {
        String normalized = normalize(userQuestion);
        String status = null;
        if (containsAny(normalized, "completado", "completados", "realizado", "realizados", "completed")) {
            status = "COMPLETED";
        } else if (containsAny(normalized, "pendiente", "pendientes", "pending")) {
            status = "PENDING";
        } else if (containsAny(normalized, "cancelado", "cancelados", "cancelled", "canceled")) {
            status = "CANCELLED";
        } else if (containsAny(normalized, "en progreso", "progreso", "in progress")) {
            status = "IN_PROGRESS";
        }
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, null, status, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos con estado " + blankToDash(status) + ".", "maintenance.byStatus", "Maintenance by status", "No maintenance records found for status.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byStatus", "Maintenance by status", "Estos son los mantenimientos con estado " + status + ":");
    }

    public AiToolAnswer maintenanceByProperty(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "propiedad", "propiedades", "casa", "bungalow", "alojamiento", "de", "del"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, search, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos para esa propiedad.", "maintenance.byProperty", "Maintenance by property", "No maintenance records found for property.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byProperty", "Maintenance by property", "Estos son los mantenimientos que encontré para esa propiedad:");
    }

    public AiToolAnswer maintenanceByCategoryOrType(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String search = nullableSearch(extractSearchText(userQuestion, "mantenimiento", "mantenimientos", "categoria", "categorias", "tipo", "tipos", "de", "del"));
        List<Map<String, Object>> rows = maintenanceRows(organizationId, null, null, search, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré mantenimientos para esa categoría o tipo.", "maintenance.byCategoryOrType", "Maintenance by category or type", "No maintenance records found for category/type.", List.of());
        }
        return maintenanceRowsAnswer(rows, "maintenance.byCategoryOrType", "Maintenance by category or type", "Estos son los mantenimientos que encontré por categoría/tipo:");
    }

    public AiToolAnswer maintenanceCostSummary(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        boolean thisMonth = containsAny(normalize(userQuestion), "este mes", "mes actual");
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost,
                       COALESCE(MAX(mr.cost), 0) AS max_cost,
                       COALESCE(MIN(mr.cost), 0) AS min_cost
                FROM maintenance_records mr
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                  AND (CAST(:fromDate AS DATE) IS NULL OR COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) >= CAST(:fromDate AS DATE))
                  AND (CAST(:toDate AS DATE) IS NULL OR COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) < CAST(:toDate AS DATE))
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("fromDate", thisMonth ? Date.valueOf(monthStart) : null);
                    q.setParameter("toDate", thisMonth ? Date.valueOf(nextMonthStart) : null);
                }, "maintenanceCount", "totalCost", "averageCost", "maxCost", "minCost");
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String period = thisMonth ? "este mes" : "en todos los mantenimientos con costo";
        String answer = "El resumen de costos de mantenimiento " + period + " es:\n"
                + "- Mantenimientos con costo: " + blankToDash(value(row.get("maintenanceCount"))) + "\n"
                + "- Total: " + formatMoney(row.get("totalCost")) + "\n"
                + "- Promedio: " + formatMoney(row.get("averageCost")) + "\n"
                + "- Máximo: " + formatMoney(row.get("maxCost")) + "\n"
                + "- Mínimo: " + formatMoney(row.get("minCost"));
        return AiToolAnswer.of(answer, "maintenance.costSummary", "Maintenance cost summary", "Maintenance cost summary was calculated.", rows);
    }

    public AiToolAnswer maintenanceCostByProperty() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT p.name AS property_name,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY p.name
                ORDER BY total_cost DESC, maintenance_count DESC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "propertyName", "maintenanceCount", "totalCost", "averageCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento por propiedad.", "maintenance.costByProperty", "Maintenance cost by property", "No maintenance cost rows found by property.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por propiedad:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageCost")));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByProperty", "Maintenance cost by property", "%d maintenance cost rows by property found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceCostByCategory() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COALESCE(mc.name, 'Sin categoría') AS category_name,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost,
                       COALESCE(AVG(mr.cost), 0) AS average_cost
                FROM maintenance_records mr
                LEFT JOIN maintenance_categories mc ON mc.id = mr.maintenance_category_id
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY COALESCE(mc.name, 'Sin categoría')
                ORDER BY total_cost DESC, maintenance_count DESC, category_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "categoryName", "maintenanceCount", "totalCost", "averageCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento por categoría.", "maintenance.costByCategory", "Maintenance cost by category", "No maintenance cost rows found by category.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por categoría:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageCost")));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByCategory", "Maintenance cost by category", "%d maintenance cost rows by category found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceCostByMonth() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT TO_CHAR(DATE_TRUNC('month', COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)), 'YYYY-MM') AS month,
                       COUNT(mr.id) AS maintenance_count,
                       COALESCE(SUM(mr.cost), 0) AS total_cost
                FROM maintenance_records mr
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND mr.cost IS NOT NULL
                GROUP BY DATE_TRUNC('month', COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at))
                ORDER BY month DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "month", "maintenanceCount", "totalCost");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré costos de mantenimiento agrupados por mes.", "maintenance.costByMonth", "Maintenance cost by month", "No maintenance cost rows found by month.", List.of());
        }
        StringBuilder answer = new StringBuilder("Costos de mantenimiento por mes:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("month"))))
                    .append(" | total: ").append(formatMoney(row.get("totalCost")))
                    .append(" | registros: ").append(blankToDash(value(row.get("maintenanceCount"))));
        }
        return AiToolAnswer.of(answer.toString(), "maintenance.costByMonth", "Maintenance cost by month", "%d maintenance cost rows by month found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer maintenanceImagesSummary(boolean withoutImages) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String havingClause = withoutImages ? "HAVING COUNT(mri.id) = 0\n" : "HAVING COUNT(mri.id) > 0\n";
        List<Map<String, Object>> rows = query("""
                SELECT mr.id,
                       p.name AS property_name,
                       mr.title,
                       mr.status,
                       COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS maintenance_date,
                       COUNT(mri.id) AS image_count
                FROM maintenance_records mr
                JOIN properties p ON p.id = mr.property_id
                LEFT JOIN maintenance_record_images mri ON mri.maintenance_record_id = mr.id
                                                       AND mri.organization_id = mr.organization_id
                                                       AND mri.deleted_at IS NULL
                                                       AND mri.status = 'ACTIVE'
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                GROUP BY mr.id, p.name, mr.title, mr.status, COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at)
                """ + havingClause + """
                ORDER BY image_count DESC, maintenance_date DESC NULLS LAST
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "propertyName", "title", "status", "maintenanceDate", "imageCount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    withoutImages ? "No encontré mantenimientos sin evidencia fotográfica." : "No encontré mantenimientos con imágenes activas.",
                    withoutImages ? "maintenance.withoutImages" : "maintenance.withImages",
                    withoutImages ? "Maintenance without images" : "Maintenance with images",
                    "No maintenance image metadata rows found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(withoutImages ? "Estos mantenimientos no tienen imágenes activas:" : "Estos mantenimientos sí tienen imágenes activas:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("maintenanceDate"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
        return AiToolAnswer.of(
                answer.toString(),
                withoutImages ? "maintenance.withoutImages" : "maintenance.withImages",
                withoutImages ? "Maintenance without images" : "Maintenance with images",
                "%d maintenance image rows found.".formatted(rows.size()),
                rows
        );
    }
}
