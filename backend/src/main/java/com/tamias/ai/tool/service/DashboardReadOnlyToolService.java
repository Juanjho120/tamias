package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardReadOnlyToolService extends AiReadOnlyToolSupport {

    public DashboardReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer operationalSummary() {
        return super.operationalSummary();
    }

    public AiToolAnswer dashboardReservationSummary() {
        return super.dashboardReservationSummary();
    }

    public AiToolAnswer dashboardMaintenanceSummary() {
        return super.dashboardMaintenanceSummary();
    }

    public AiToolAnswer dashboardPurchaseSummary() {
        return super.dashboardPurchaseSummary();
    }

    public AiToolAnswer dashboardTaskSummary() {
        return super.dashboardTaskSummary();
    }

    public AiToolAnswer dashboardDocumentSummary() {
        return super.dashboardDocumentSummary();
    }

    public AiToolAnswer dashboardCalendarEvents() {
        return super.dashboardCalendarEvents();
    }

    public AiToolAnswer dashboardAlertSummary() {
        return super.dashboardAlertSummary();
    }

    public AiToolAnswer dashboardAttentionToday() {
        return super.dashboardAttentionToday();
    }

}
