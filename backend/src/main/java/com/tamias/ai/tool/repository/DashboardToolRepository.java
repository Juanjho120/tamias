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
public class DashboardToolRepository extends AiReadOnlyToolSupport {

    public DashboardToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer operationalSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate nextSevenDays = today.plusDays(7);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activeProperties", scalar("""
                SELECT COUNT(*)
                FROM properties
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                """, q -> q.setParameter("organizationId", organizationId)));
        summary.put("upcomingReservations7Days", scalar("""
                SELECT COUNT(*)
                FROM reservations
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND check_in BETWEEN :today AND :nextSevenDays
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("nextSevenDays", Date.valueOf(nextSevenDays));
                }));
        summary.put("overdueScheduledMaintenance", scalar("""
                SELECT COUNT(*)
                FROM scheduled_maintenance
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status = 'ACTIVE'
                  AND next_due_date < :today
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }));
        summary.put("openTaskLists", scalar("""
                SELECT COUNT(*)
                FROM task_lists
                WHERE organization_id = :organizationId
                  AND deleted_at IS NULL
                  AND status IN ('OPEN', 'IN_PROGRESS')
                """, q -> q.setParameter("organizationId", organizationId)));
        summary.put("documentsNotIndexed", scalar("""
                SELECT COUNT(*)
                FROM documents d
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM document_chunks dc
                      WHERE dc.document_id = d.id
                        AND dc.organization_id = d.organization_id
                        AND dc.vector_store_id IS NOT NULL
                  )
                """, q -> q.setParameter("organizationId", organizationId)));

        List<Map<String, Object>> rows = List.of(summary);
        String answer = """
                Así va tu operación hoy:
                - Propiedades activas: %s
                - Reservas con check-in en los próximos 7 días: %s
                - Mantenimientos programados vencidos: %s
                - Listas de tareas abiertas/en progreso: %s
                - Documentos sin evidencia de indexación IA: %s
                """.formatted(
                summary.get("activeProperties"),
                summary.get("upcomingReservations7Days"),
                summary.get("overdueScheduledMaintenance"),
                summary.get("openTaskLists"),
                summary.get("documentsNotIndexed")
        ).trim();
        return AiToolAnswer.of(
                answer,
                "dashboard.operationalSummary",
                "Operational summary",
                "Operational counters were calculated.",
                rows
        );
    }

    public AiToolAnswer dashboardReservationSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) FILTER (WHERE r.status = 'ACTIVE') AS active_count,
                       COUNT(*) FILTER (WHERE r.status = 'ACTIVE' AND CURRENT_DATE BETWEEN r.check_in AND r.check_out) AS current_count,
                       COUNT(*) FILTER (WHERE r.status = 'ACTIVE' AND r.check_in BETWEEN :today AND :weekEnd) AS next_7_days_count,
                       COUNT(*) FILTER (WHERE r.status = 'ACTIVE' AND r.check_in BETWEEN :monthStart AND :monthEnd) AS this_month_count,
                       COALESCE(SUM(CASE WHEN r.status = 'ACTIVE' AND r.check_in BETWEEN :monthStart AND :monthEnd THEN r.reservation_value ELSE 0 END), 0) AS this_month_value
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("weekEnd", Date.valueOf(weekEnd));
                    q.setParameter("monthStart", Date.valueOf(monthStart));
                    q.setParameter("monthEnd", Date.valueOf(monthEnd));
                }, "activeCount", "currentCount", "next7DaysCount", "thisMonthCount", "thisMonthValue");
        Map<String, Object> row = rows.get(0);
        String answer = "Resumen de reservaciones:" + System.lineSeparator()
                + "- Reservaciones activas: " + blankToDash(value(row.get("activeCount"))) + System.lineSeparator()
                + "- Reservaciones en curso hoy: " + blankToDash(value(row.get("currentCount"))) + System.lineSeparator()
                + "- Check-ins en los próximos 7 días: " + blankToDash(value(row.get("next7DaysCount"))) + System.lineSeparator()
                + "- Check-ins este mes: " + blankToDash(value(row.get("thisMonthCount"))) + System.lineSeparator()
                + "- Valor de reservaciones con check-in este mes: " + formatMoney(row.get("thisMonthValue"));
        return AiToolAnswer.of(answer, "dashboard.reservationSummary", "Reservation dashboard summary", "Reservation dashboard counters were calculated.", rows);
    }

    public AiToolAnswer dashboardMaintenanceSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(*) AS maintenance_count,
                       COUNT(*) FILTER (WHERE mr.status = 'COMPLETED') AS completed_count,
                       COUNT(*) FILTER (WHERE mr.status <> 'COMPLETED') AS open_count,
                       COALESCE(SUM(COALESCE(mr.cost, 0)), 0) AS total_cost,
                       COUNT(*) FILTER (WHERE EXISTS (
                           SELECT 1 FROM maintenance_record_images mri
                           WHERE mri.maintenance_record_id = mr.id
                             AND mri.organization_id = mr.organization_id
                             AND mri.deleted_at IS NULL
                             AND mri.status = 'ACTIVE'
                       )) AS with_images_count,
                       COUNT(*) FILTER (WHERE mr.status = 'COMPLETED' AND mr.performed_at >= :monthStart) AS completed_this_month
                FROM maintenance_records mr
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("monthStart", Date.valueOf(today.withDayOfMonth(1)));
                }, "maintenanceCount", "completedCount", "openCount", "totalCost", "withImagesCount", "completedThisMonth");
        Map<String, Object> row = rows.get(0);
        String answer = "Resumen de mantenimiento:" + System.lineSeparator()
                + "- Registros totales: " + blankToDash(value(row.get("maintenanceCount"))) + System.lineSeparator()
                + "- Completados: " + blankToDash(value(row.get("completedCount"))) + System.lineSeparator()
                + "- Abiertos/no completados: " + blankToDash(value(row.get("openCount"))) + System.lineSeparator()
                + "- Completados este mes: " + blankToDash(value(row.get("completedThisMonth"))) + System.lineSeparator()
                + "- Con evidencia fotográfica: " + blankToDash(value(row.get("withImagesCount"))) + System.lineSeparator()
                + "- Costo total registrado: " + formatMoney(row.get("totalCost"));
        return AiToolAnswer.of(answer, "dashboard.maintenanceSummary", "Maintenance dashboard summary", "Maintenance dashboard counters were calculated.", rows);
    }

    public AiToolAnswer dashboardPurchaseSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT pl.id) AS purchase_list_count,
                       COUNT(DISTINCT pl.id) FILTER (WHERE pl.status = 'COMPLETED') AS completed_list_count,
                       COUNT(pi.id) AS item_count,
                       COUNT(pi.id) FILTER (WHERE pi.purchased = TRUE) AS purchased_item_count,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE THEN pi.estimated_price ELSE 0 END), 0) AS purchased_total_cost,
                       COALESCE(SUM(CASE WHEN pi.purchased = TRUE AND pl.purchase_date >= :monthStart THEN pi.estimated_price ELSE 0 END), 0) AS purchased_this_month_cost
                FROM purchase_lists pl
                LEFT JOIN purchase_items pi ON pi.purchase_list_id = pl.id
                                           AND pi.organization_id = pl.organization_id
                WHERE pl.organization_id = :organizationId
                  AND pl.deleted_at IS NULL
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("monthStart", Date.valueOf(monthStart));
                }, "purchaseListCount", "completedListCount", "itemCount", "purchasedItemCount", "purchasedTotalCost", "purchasedThisMonthCost");
        Map<String, Object> row = rows.get(0);
        String answer = "Resumen de compras:" + System.lineSeparator()
                + "- Listas de compras: " + blankToDash(value(row.get("purchaseListCount"))) + System.lineSeparator()
                + "- Listas completadas: " + blankToDash(value(row.get("completedListCount"))) + System.lineSeparator()
                + "- Items registrados: " + blankToDash(value(row.get("itemCount"))) + System.lineSeparator()
                + "- Items marcados como comprados: " + blankToDash(value(row.get("purchasedItemCount"))) + System.lineSeparator()
                + "- Gasto comprado total: " + formatMoney(row.get("purchasedTotalCost")) + System.lineSeparator()
                + "- Gasto comprado este mes: " + formatMoney(row.get("purchasedThisMonthCost"));
        return AiToolAnswer.of(answer, "dashboard.purchaseSummary", "Purchase dashboard summary", "Purchase dashboard counters were calculated.", rows);
    }

    public AiToolAnswer dashboardTaskSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT tl.id) AS task_list_count,
                       COUNT(DISTINCT tl.id) FILTER (WHERE tl.status IN ('OPEN', 'IN_PROGRESS')) AS active_list_count,
                       COUNT(DISTINCT tl.id) FILTER (WHERE tl.status = 'COMPLETED') AS completed_list_count,
                       COUNT(DISTINCT tl.id) FILTER (WHERE tl.due_date < :today AND tl.status IN ('OPEN', 'IN_PROGRESS')) AS overdue_list_count,
                       COUNT(ti.id) AS task_item_count,
                       COUNT(ti.id) FILTER (WHERE ti.completed = TRUE) AS completed_item_count,
                       COUNT(ti.id) FILTER (WHERE ti.completed = FALSE) AS pending_item_count
                FROM task_lists tl
                LEFT JOIN task_items ti ON ti.task_list_id = tl.id
                                       AND ti.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                }, "taskListCount", "activeListCount", "completedListCount", "overdueListCount", "taskItemCount", "completedItemCount", "pendingItemCount");
        Map<String, Object> row = rows.get(0);
        String answer = "Resumen de tareas:" + System.lineSeparator()
                + "- Listas de tareas: " + blankToDash(value(row.get("taskListCount"))) + System.lineSeparator()
                + "- Listas activas: " + blankToDash(value(row.get("activeListCount"))) + System.lineSeparator()
                + "- Listas completadas: " + blankToDash(value(row.get("completedListCount"))) + System.lineSeparator()
                + "- Listas vencidas: " + blankToDash(value(row.get("overdueListCount"))) + System.lineSeparator()
                + "- Tareas específicas: " + blankToDash(value(row.get("taskItemCount"))) + System.lineSeparator()
                + "- Tareas completadas: " + blankToDash(value(row.get("completedItemCount"))) + System.lineSeparator()
                + "- Tareas pendientes: " + blankToDash(value(row.get("pendingItemCount")));
        return AiToolAnswer.of(answer, "dashboard.taskSummary", "Task dashboard summary", "Task dashboard counters were calculated.", rows);
    }

    public AiToolAnswer dashboardDocumentSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(DISTINCT d.id) AS document_count,
                       COUNT(DISTINCT d.id) FILTER (WHERE d.processing_status = 'PROCESSED') AS processed_count,
                       COUNT(DISTINCT d.id) FILTER (WHERE d.processing_status = 'FAILED') AS failed_count,
                       COUNT(DISTINCT d.id) FILTER (WHERE d.processing_status IN ('PENDING', 'PROCESSING')) AS pending_count,
                       COUNT(dc.id) AS chunk_count,
                       COUNT(dc.id) FILTER (WHERE dc.vector_store_id IS NOT NULL) AS indexed_chunk_count
                FROM documents d
                LEFT JOIN document_chunks dc ON dc.document_id = d.id
                                            AND dc.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                """, q -> q.setParameter("organizationId", organizationId),
                "documentCount", "processedCount", "failedCount", "pendingCount", "chunkCount", "indexedChunkCount");
        Map<String, Object> row = rows.get(0);
        String answer = "Resumen de documentos:" + System.lineSeparator()
                + "- Documentos cargados: " + blankToDash(value(row.get("documentCount"))) + System.lineSeparator()
                + "- Procesados: " + blankToDash(value(row.get("processedCount"))) + System.lineSeparator()
                + "- Fallidos: " + blankToDash(value(row.get("failedCount"))) + System.lineSeparator()
                + "- Pendientes/en proceso: " + blankToDash(value(row.get("pendingCount"))) + System.lineSeparator()
                + "- Chunks generados: " + blankToDash(value(row.get("chunkCount"))) + System.lineSeparator()
                + "- Chunks con vector_store_id: " + blankToDash(value(row.get("indexedChunkCount")));
        return AiToolAnswer.of(answer, "dashboard.documentSummary", "Document dashboard summary", "Document dashboard counters were calculated.", rows);
    }

    public AiToolAnswer dashboardCalendarEvents() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(14);
        List<Map<String, Object>> rows = query("""
                SELECT 'CHECK_IN' AS event_type,
                       r.check_in AS event_date,
                       CONCAT('Check-in ', COALESCE(r.reservation_code, 'sin código')) AS title,
                       p.name AS property_name
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in BETWEEN :today AND :until
                UNION ALL
                SELECT 'CHECK_OUT' AS event_type,
                       r.check_out AS event_date,
                       CONCAT('Check-out ', COALESCE(r.reservation_code, 'sin código')) AS title,
                       p.name AS property_name
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_out BETWEEN :today AND :until
                UNION ALL
                SELECT 'SCHEDULED_MAINTENANCE' AS event_type,
                       sm.next_due_date AS event_date,
                       sm.title,
                       p.name AS property_name
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id AND p.organization_id = sm.organization_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND sm.next_due_date BETWEEN :today AND :until
                UNION ALL
                SELECT 'TASK_LIST' AS event_type,
                       tl.due_date AS event_date,
                       tl.title,
                       p.name AS property_name
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND tl.due_date BETWEEN :today AND :until
                ORDER BY event_date ASC, event_type ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("until", Date.valueOf(until));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "eventType", "eventDate", "title", "propertyName");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré eventos operativos en los próximos 14 días.", "dashboard.calendarEvents", "Dashboard calendar events", "No dashboard calendar events found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son los próximos eventos operativos del calendario:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("eventDate"))))
                    .append(" | ").append(blankToDash(value(row.get("eventType"))))
                    .append(" | ").append(blankToDash(value(row.get("title"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))));
        }
        return AiToolAnswer.of(answer.toString(), "dashboard.calendarEvents", "Dashboard calendar events", "%d calendar events found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer dashboardAlertSummary() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Object overdueScheduledCount = scalar("""
                SELECT COUNT(*)
                FROM scheduled_maintenance sm
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND sm.next_due_date < :today
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                });

        Object overdueTaskListCount = scalar("""
                SELECT COUNT(*)
                FROM task_lists tl
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND tl.due_date < :today
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                });

        Object overdueTaskItemCount = scalar("""
                SELECT COUNT(*)
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                WHERE ti.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND ti.completed = FALSE
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND tl.due_date < :today
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                });

        Object failedDocumentCount = scalar("""
                SELECT COUNT(*)
                FROM documents d
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND d.processing_status = 'FAILED'
                """, q -> q.setParameter("organizationId", organizationId));

        Object processedNotIndexedCount = scalar("""
                SELECT COUNT(*)
                FROM documents d
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND d.processing_status = 'PROCESSED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM document_chunks dc
                      WHERE dc.document_id = d.id
                        AND dc.organization_id = d.organization_id
                        AND dc.vector_store_id IS NOT NULL
                  )
                """, q -> q.setParameter("organizationId", organizationId));

        Object checkinsNext24hCount = scalar("""
                SELECT COUNT(*)
                FROM reservations r
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in BETWEEN :today AND :tomorrow
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("tomorrow", Date.valueOf(tomorrow));
                });

        String answer = """
                Alertas operativas actuales:
                - Mantenimientos programados vencidos: %s
                - Listas de tareas vencidas: %s
                - Tareas específicas vencidas: %s
                - Documentos con procesamiento fallido: %s
                - Documentos procesados sin indexación IA: %s
                - Check-ins en las próximas 24 horas: %s
                """.formatted(
                blankToDash(value(overdueScheduledCount)),
                blankToDash(value(overdueTaskListCount)),
                blankToDash(value(overdueTaskItemCount)),
                blankToDash(value(failedDocumentCount)),
                blankToDash(value(processedNotIndexedCount)),
                blankToDash(value(checkinsNext24hCount))
        ).trim();

        List<Map<String, Object>> evidenceRows = new ArrayList<>();
        evidenceRows.add(Map.of("alertType", "overdueScheduledMaintenance", "count", value(overdueScheduledCount)));
        evidenceRows.add(Map.of("alertType", "overdueTaskLists", "count", value(overdueTaskListCount)));
        evidenceRows.add(Map.of("alertType", "overdueTaskItems", "count", value(overdueTaskItemCount)));
        evidenceRows.add(Map.of("alertType", "failedDocuments", "count", value(failedDocumentCount)));
        evidenceRows.add(Map.of("alertType", "processedNotIndexedDocuments", "count", value(processedNotIndexedCount)));
        evidenceRows.add(Map.of("alertType", "checkinsNext24h", "count", value(checkinsNext24hCount)));

        return AiToolAnswer.of(answer, "dashboard.alertSummary", "Dashboard alert summary", "Operational alert counters were calculated without detail rows.", evidenceRows);
    }

    public AiToolAnswer dashboardAttentionToday() {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Map<String, Object>> overdueScheduled = query("""
                SELECT sm.title,
                       p.name AS property_name,
                       sm.next_due_date
                FROM scheduled_maintenance sm
                JOIN properties p ON p.id = sm.property_id AND p.organization_id = sm.organization_id
                WHERE sm.organization_id = :organizationId
                  AND sm.deleted_at IS NULL
                  AND sm.status = 'ACTIVE'
                  AND sm.next_due_date < :today
                ORDER BY sm.next_due_date ASC, sm.title ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "title", "propertyName", "nextDueDate");

        List<Map<String, Object>> overdueTaskLists = query("""
                SELECT tl.title,
                       p.name AS property_name,
                       tl.due_date
                FROM task_lists tl
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                WHERE tl.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND tl.due_date < :today
                ORDER BY tl.due_date ASC, tl.title ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "title", "propertyName", "dueDate");

        List<Map<String, Object>> overdueTaskItems = query("""
                SELECT ti.task_name,
                       tl.title AS task_list_title,
                       p.name AS property_name,
                       tl.due_date
                FROM task_items ti
                JOIN task_lists tl ON tl.id = ti.task_list_id AND tl.organization_id = ti.organization_id
                JOIN properties p ON p.id = tl.property_id AND p.organization_id = tl.organization_id
                WHERE ti.organization_id = :organizationId
                  AND tl.deleted_at IS NULL
                  AND ti.completed = FALSE
                  AND tl.status IN ('OPEN', 'IN_PROGRESS')
                  AND tl.due_date < :today
                ORDER BY tl.due_date ASC, ti.sort_order ASC, ti.task_name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "taskName", "taskListTitle", "propertyName", "dueDate");

        List<Map<String, Object>> failedDocuments = query("""
                SELECT d.title,
                       d.document_type,
                       COALESCE(p.name, 'Sin propiedad') AS property_name
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND d.processing_status = 'FAILED'
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "title", "documentType", "propertyName");

        List<Map<String, Object>> processedNotIndexed = query("""
                SELECT d.title,
                       d.document_type,
                       COALESCE(p.name, 'Sin propiedad') AS property_name
                FROM documents d
                LEFT JOIN properties p ON p.id = d.property_id AND p.organization_id = d.organization_id
                WHERE d.organization_id = :organizationId
                  AND d.deleted_at IS NULL
                  AND d.processing_status = 'PROCESSED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM document_chunks dc
                      WHERE dc.document_id = d.id
                        AND dc.organization_id = d.organization_id
                        AND dc.vector_store_id IS NOT NULL
                  )
                ORDER BY d.created_at DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "title", "documentType", "propertyName");

        List<Map<String, Object>> checkinsNext24h = query("""
                SELECT r.reservation_code,
                       p.name AS property_name,
                       r.check_in,
                       r.check_out
                FROM reservations r
                JOIN properties p ON p.id = r.property_id AND p.organization_id = r.organization_id
                WHERE r.organization_id = :organizationId
                  AND r.deleted_at IS NULL
                  AND r.status = 'ACTIVE'
                  AND r.check_in BETWEEN :today AND :tomorrow
                ORDER BY r.check_in ASC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("today", Date.valueOf(today));
                    q.setParameter("tomorrow", Date.valueOf(tomorrow));
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "reservationCode", "propertyName", "checkIn", "checkOut");

        StringBuilder answer = new StringBuilder("Alertas operativas actuales:");
        appendAlertGroup(answer, "Mantenimientos programados vencidos", overdueScheduled, row ->
                blankToDash(value(row.get("title"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))) + " | vencía: " + blankToDash(value(row.get("nextDueDate"))));
        appendAlertGroup(answer, "Listas de tareas vencidas", overdueTaskLists, row ->
                blankToDash(value(row.get("title"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))) + " | vencía: " + blankToDash(value(row.get("dueDate"))));
        appendAlertGroup(answer, "Tareas específicas vencidas", overdueTaskItems, row ->
                blankToDash(value(row.get("taskName"))) + " | lista: " + blankToDash(value(row.get("taskListTitle"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))) + " | vencía: " + blankToDash(value(row.get("dueDate"))));
        appendAlertGroup(answer, "Documentos con procesamiento fallido", failedDocuments, row ->
                blankToDash(value(row.get("title"))) + " | tipo: " + blankToDash(value(row.get("documentType"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))));
        appendAlertGroup(answer, "Documentos procesados sin indexación IA", processedNotIndexed, row ->
                blankToDash(value(row.get("title"))) + " | tipo: " + blankToDash(value(row.get("documentType"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))));
        appendAlertGroup(answer, "Check-ins en las próximas 24 horas", checkinsNext24h, row ->
                blankToDash(value(row.get("reservationCode"))) + " | propiedad: " + blankToDash(value(row.get("propertyName"))) + " | check-in: " + blankToDash(value(row.get("checkIn"))) + " | check-out: " + blankToDash(value(row.get("checkOut"))));

        List<Map<String, Object>> evidenceRows = new ArrayList<>();
        evidenceRows.add(Map.of("alertType", "overdueScheduledMaintenance", "count", overdueScheduled.size()));
        evidenceRows.add(Map.of("alertType", "overdueTaskLists", "count", overdueTaskLists.size()));
        evidenceRows.add(Map.of("alertType", "overdueTaskItems", "count", overdueTaskItems.size()));
        evidenceRows.add(Map.of("alertType", "failedDocuments", "count", failedDocuments.size()));
        evidenceRows.add(Map.of("alertType", "processedNotIndexedDocuments", "count", processedNotIndexed.size()));
        evidenceRows.add(Map.of("alertType", "checkinsNext24h", "count", checkinsNext24h.size()));

        return AiToolAnswer.of(answer.toString(), "dashboard.attentionToday", "Dashboard attention today", "Operational alert counters and details were calculated for attention today.", evidenceRows);
    }
}
