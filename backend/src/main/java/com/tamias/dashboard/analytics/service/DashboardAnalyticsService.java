package com.tamias.dashboard.analytics.service;

import com.tamias.dashboard.analytics.dto.DashboardAnalyticsResponse;
import com.tamias.dashboard.analytics.dto.DashboardKpiResponse;
import com.tamias.dashboard.analytics.dto.MonthlyAmountResponse;
import com.tamias.dashboard.analytics.dto.MonthlyCountAmountResponse;
import com.tamias.dashboard.analytics.dto.TopItemResponse;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardAnalyticsService {
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final CurrentUserService currentUserService;

    @PersistenceContext
    private EntityManager entityManager;

    public DashboardAnalyticsService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public DashboardAnalyticsResponse getAnalytics(Integer months, Integer upcomingDays, Integer topLimit) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        int safeMonths = clamp(months, 1, 24, 6);
        int safeUpcomingDays = clamp(upcomingDays, 1, 365, 30);
        int safeTopLimit = clamp(topLimit, 1, 20, 5);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate fromDate = YearMonth.from(today)
            .minusMonths(safeMonths - 1L)
            .atDay(1);
        LocalDate toDateExclusive = today.plusDays(1);
        LocalDate upcomingUntil = today.plusDays(safeUpcomingDays);

        return new DashboardAnalyticsResponse(
            getKpis(organizationId, today, upcomingUntil),
            getMaintenanceCostByMonth(organizationId, fromDate, toDateExclusive),
            getPurchaseCostByMonth(organizationId, fromDate, toDateExclusive),
            getReservationsByMonth(organizationId, fromDate, toDateExclusive),
            getTopReservationSupplies(organizationId, fromDate, toDateExclusive, safeTopLimit),
            getTopPurchasedItems(organizationId, fromDate, toDateExclusive, safeTopLimit)
        );
    }

    private DashboardKpiResponse getKpis(UUID organizationId, LocalDate today, LocalDate upcomingUntil) {
        return new DashboardKpiResponse(
            countActiveReservationsToday(organizationId, today),
            countPendingMaintenanceRecords(organizationId),
            countOverdueTaskLists(organizationId, today),
            countUpcomingScheduledMaintenance(organizationId, today, upcomingUntil),
            countOpenPurchaseLists(organizationId),
            estimatedOpenPurchaseTotal(organizationId)
        );
    }

    private List<MonthlyAmountResponse> getMaintenanceCostByMonth(
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive
    ) {
        String sql = """
            SELECT date_trunc('month', COALESCE(m.performed_at, m.scheduled_at, m.created_at)) AS month_value,
                   COALESCE(SUM(m.cost), 0) AS amount
            FROM maintenance_records m
            WHERE m.organization_id = :organizationId
              AND m.deleted_at IS NULL
              AND m.status <> 'DELETED'
              AND m.cost IS NOT NULL
              AND COALESCE(m.performed_at, m.scheduled_at, m.created_at) >= :fromDate
              AND COALESCE(m.performed_at, m.scheduled_at, m.created_at) < :toDateExclusive
            GROUP BY month_value
            ORDER BY month_value
            """;

        return getResultList(sql, organizationId, fromDate, toDateExclusive)
            .stream()
            .map(row -> {
                Object[] values = (Object[]) row;

                return new MonthlyAmountResponse(
                    toMonth(values[0]),
                    toBigDecimal(values[1])
                );
            })
            .toList();
    }

    private List<MonthlyAmountResponse> getPurchaseCostByMonth(
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive
    ) {
        String sql = """
            SELECT date_trunc('month', p.purchase_date) AS month_value,
                   COALESCE(SUM(COALESCE(i.estimated_price, 0) * COALESCE(i.quantity, 0)), 0) AS amount
            FROM purchase_lists p
            JOIN purchase_items i ON i.purchase_list_id = p.id
            WHERE p.organization_id = :organizationId
              AND i.organization_id = :organizationId
              AND p.deleted_at IS NULL
              AND p.status <> 'DELETED'
              AND p.purchase_date >= :fromDate
              AND p.purchase_date < :toDateExclusive
            GROUP BY month_value
            ORDER BY month_value
            """;

        return getResultList(sql, organizationId, fromDate, toDateExclusive)
            .stream()
            .map(row -> {
                Object[] values = (Object[]) row;

                return new MonthlyAmountResponse(
                    toMonth(values[0]),
                    toBigDecimal(values[1])
                );
            })
            .toList();
    }

    private List<MonthlyCountAmountResponse> getReservationsByMonth(
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive
    ) {
        String sql = """
            SELECT date_trunc('month', r.check_in) AS month_value,
                   COUNT(r.id) AS reservation_count,
                   COALESCE(SUM(r.reservation_value), 0) AS amount
            FROM reservations r
            WHERE r.organization_id = :organizationId
              AND r.deleted_at IS NULL
              AND r.status = 'ACTIVE'
              AND r.check_in >= :fromDate
              AND r.check_in < :toDateExclusive
            GROUP BY month_value
            ORDER BY month_value
            """;

        return getResultList(sql, organizationId, fromDate, toDateExclusive)
            .stream()
            .map(row -> {
                Object[] values = (Object[]) row;

                return new MonthlyCountAmountResponse(
                    toMonth(values[0]),
                    toLong(values[1]),
                    toBigDecimal(values[2])
                );
            })
            .toList();
    }

    private List<TopItemResponse> getTopReservationSupplies(
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive,
        int limit
    ) {
        String sql = """
            SELECT COALESCE(rs.item_name_snapshot, ii.name, 'Unknown') AS item_name,
                   COALESCE(SUM(rs.quantity), 0) AS quantity,
                   CAST(0 AS numeric) AS amount
            FROM reservation_supplies rs
            JOIN reservations r ON r.id = rs.reservation_id
            LEFT JOIN inventory_items ii ON ii.id = rs.inventory_item_id
            WHERE rs.organization_id = :organizationId
              AND r.organization_id = :organizationId
              AND r.deleted_at IS NULL
              AND r.status = 'ACTIVE'
              AND r.check_in >= :fromDate
              AND r.check_in < :toDateExclusive
            GROUP BY item_name
            ORDER BY quantity DESC, item_name ASC
            LIMIT :limit
            """;

        return getLimitedResultList(sql, organizationId, fromDate, toDateExclusive, limit)
            .stream()
            .map(row -> {
                Object[] values = (Object[]) row;

                return new TopItemResponse(
                    toStringValue(values[0]),
                    toBigDecimal(values[1]),
                    toBigDecimal(values[2])
                );
            })
            .toList();
    }

    private List<TopItemResponse> getTopPurchasedItems(
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive,
        int limit
    ) {
        String sql = """
            SELECT COALESCE(pi.item_name_snapshot, ii.name, 'Unknown') AS item_name,
                   COALESCE(SUM(pi.quantity), 0) AS quantity,
                   COALESCE(SUM(COALESCE(pi.estimated_price, 0) * COALESCE(pi.quantity, 0)), 0) AS amount
            FROM purchase_items pi
            JOIN purchase_lists pl ON pl.id = pi.purchase_list_id
            LEFT JOIN inventory_items ii ON ii.id = pi.inventory_item_id
            WHERE pi.organization_id = :organizationId
              AND pl.organization_id = :organizationId
              AND pl.deleted_at IS NULL
              AND pl.status <> 'DELETED'
              AND pl.purchase_date >= :fromDate
              AND pl.purchase_date < :toDateExclusive
            GROUP BY item_name
            ORDER BY quantity DESC, item_name ASC
            LIMIT :limit
            """;

        return getLimitedResultList(sql, organizationId, fromDate, toDateExclusive, limit)
            .stream()
            .map(row -> {
                Object[] values = (Object[]) row;

                return new TopItemResponse(
                    toStringValue(values[0]),
                    toBigDecimal(values[1]),
                    toBigDecimal(values[2])
                );
            })
            .toList();
    }

    private long countActiveReservationsToday(UUID organizationId, LocalDate today) {
        String sql = """
            SELECT COUNT(r.id)
            FROM reservations r
            WHERE r.organization_id = :organizationId
              AND r.deleted_at IS NULL
              AND r.status = 'ACTIVE'
              AND r.check_in <= :today
              AND r.check_out > :today
            """;

        return toLong(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .setParameter("today", today)
            .getSingleResult());
    }

    private long countPendingMaintenanceRecords(UUID organizationId) {
        String sql = """
            SELECT COUNT(m.id)
            FROM maintenance_records m
            WHERE m.organization_id = :organizationId
              AND m.deleted_at IS NULL
              AND m.status IN ('PENDING', 'IN_PROGRESS')
            """;

        return toLong(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .getSingleResult());
    }

    private long countOverdueTaskLists(UUID organizationId, LocalDate today) {
        String sql = """
            SELECT COUNT(t.id)
            FROM task_lists t
            WHERE t.organization_id = :organizationId
              AND t.deleted_at IS NULL
              AND t.due_date IS NOT NULL
              AND t.due_date < :today
              AND t.status NOT IN ('COMPLETED', 'CANCELLED', 'DELETED')
            """;

        return toLong(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .setParameter("today", today)
            .getSingleResult());
    }

    private long countUpcomingScheduledMaintenance(UUID organizationId, LocalDate today, LocalDate upcomingUntil) {
        String sql = """
            SELECT COUNT(s.id)
            FROM scheduled_maintenance s
            WHERE s.organization_id = :organizationId
              AND s.deleted_at IS NULL
              AND s.status = 'ACTIVE'
              AND s.next_due_date >= :today
              AND s.next_due_date <= :upcomingUntil
            """;

        return toLong(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .setParameter("today", today)
            .setParameter("upcomingUntil", upcomingUntil)
            .getSingleResult());
    }

    private long countOpenPurchaseLists(UUID organizationId) {
        String sql = """
            SELECT COUNT(p.id)
            FROM purchase_lists p
            WHERE p.organization_id = :organizationId
              AND p.deleted_at IS NULL
              AND p.status IN ('OPEN', 'PARTIALLY_PURCHASED')
            """;

        return toLong(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .getSingleResult());
    }

    private BigDecimal estimatedOpenPurchaseTotal(UUID organizationId) {
        String sql = """
            SELECT COALESCE(SUM(COALESCE(i.estimated_price, 0) * COALESCE(i.quantity, 0)), 0)
            FROM purchase_lists p
            JOIN purchase_items i ON i.purchase_list_id = p.id
            WHERE p.organization_id = :organizationId
              AND i.organization_id = :organizationId
              AND p.deleted_at IS NULL
              AND p.status IN ('OPEN', 'PARTIALLY_PURCHASED')
            """;

        return toBigDecimal(entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .getSingleResult());
    }

    private List<?> getResultList(
        String sql,
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive
    ) {
        Query query = entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .setParameter("fromDate", fromDate)
            .setParameter("toDateExclusive", toDateExclusive);

        return query.getResultList();
    }

    private List<?> getLimitedResultList(
        String sql,
        UUID organizationId,
        LocalDate fromDate,
        LocalDate toDateExclusive,
        int limit
    ) {
        Query query = entityManager.createNativeQuery(sql)
            .setParameter("organizationId", organizationId)
            .setParameter("fromDate", fromDate)
            .setParameter("toDateExclusive", toDateExclusive)
            .setParameter("limit", limit);

        return query.getResultList();
    }

    private int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private String toMonth(Object value) {
        if (value instanceof Timestamp timestamp) {
            return YearMonth.from(timestamp.toLocalDateTime()).format(MONTH_FORMATTER);
        }

        if (value instanceof Date date) {
            return YearMonth.from(date.toLocalDate()).format(MONTH_FORMATTER);
        }

        if (value instanceof LocalDate localDate) {
            return YearMonth.from(localDate).format(MONTH_FORMATTER);
        }

        if (value instanceof java.time.LocalDateTime localDateTime) {
            return YearMonth.from(localDateTime).format(MONTH_FORMATTER);
        }

        return String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
