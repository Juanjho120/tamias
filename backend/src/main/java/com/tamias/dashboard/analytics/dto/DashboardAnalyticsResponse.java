package com.tamias.dashboard.analytics.dto;

import java.util.List;

public record DashboardAnalyticsResponse(
    DashboardKpiResponse kpis,
    List<MonthlyAmountResponse> maintenanceCostByMonth,
    List<MonthlyAmountResponse> purchaseCostByMonth,
    List<MonthlyCountAmountResponse> reservationsByMonth,
    List<TopItemResponse> topReservationSupplies,
    List<TopItemResponse> topPurchasedItems
) {
}
