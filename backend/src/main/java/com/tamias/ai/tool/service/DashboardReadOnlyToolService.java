package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.DashboardToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardReadOnlyToolService {

    private final DashboardToolRepository repository;

    public DashboardReadOnlyToolService(DashboardToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer operationalSummary() {
        return repository.operationalSummary();
    }

    public AiToolAnswer dashboardReservationSummary() {
        return repository.dashboardReservationSummary();
    }

    public AiToolAnswer dashboardMaintenanceSummary() {
        return repository.dashboardMaintenanceSummary();
    }

    public AiToolAnswer dashboardPurchaseSummary() {
        return repository.dashboardPurchaseSummary();
    }

    public AiToolAnswer dashboardTaskSummary() {
        return repository.dashboardTaskSummary();
    }

    public AiToolAnswer dashboardDocumentSummary() {
        return repository.dashboardDocumentSummary();
    }

    public AiToolAnswer dashboardCalendarEvents() {
        return repository.dashboardCalendarEvents();
    }

    public AiToolAnswer dashboardAlertSummary() {
        return repository.dashboardAlertSummary();
    }

    public AiToolAnswer dashboardAttentionToday() {
        return repository.dashboardAttentionToday();
    }
}
