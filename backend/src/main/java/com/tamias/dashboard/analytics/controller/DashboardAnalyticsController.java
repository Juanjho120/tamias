package com.tamias.dashboard.analytics.controller;

import com.tamias.dashboard.analytics.dto.DashboardAnalyticsResponse;
import com.tamias.dashboard.analytics.service.DashboardAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/analytics")
public class DashboardAnalyticsController {
    private final DashboardAnalyticsService dashboardAnalyticsService;

    public DashboardAnalyticsController(DashboardAnalyticsService dashboardAnalyticsService) {
        this.dashboardAnalyticsService = dashboardAnalyticsService;
    }

    @GetMapping
    public DashboardAnalyticsResponse getAnalytics(
        @RequestParam(defaultValue = "6") Integer months,
        @RequestParam(defaultValue = "30") Integer upcomingDays,
        @RequestParam(defaultValue = "5") Integer topLimit
    ) {
        return dashboardAnalyticsService.getAnalytics(months, upcomingDays, topLimit);
    }
}
