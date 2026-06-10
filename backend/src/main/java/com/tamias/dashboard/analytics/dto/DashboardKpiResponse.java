package com.tamias.dashboard.analytics.dto;

import java.math.BigDecimal;

public record DashboardKpiResponse(
    long activeReservationsToday,
    long pendingMaintenanceRecords,
    long overdueTaskLists,
    long upcomingScheduledMaintenance,
    long openPurchaseLists,
    BigDecimal estimatedOpenPurchaseTotal
) {
}
