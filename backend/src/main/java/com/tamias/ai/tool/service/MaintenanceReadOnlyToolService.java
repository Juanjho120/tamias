package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaintenanceReadOnlyToolService extends AiReadOnlyToolSupport {

    public MaintenanceReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer lastPerformedMaintenance(String userQuestion) {
        return super.lastPerformedMaintenance(userQuestion);
    }

    public AiToolAnswer maintenanceSearch(String userQuestion) {
        return super.maintenanceSearch(userQuestion);
    }

    public AiToolAnswer recentMaintenance() {
        return super.recentMaintenance();
    }

    public AiToolAnswer maintenanceByStatus(String userQuestion) {
        return super.maintenanceByStatus(userQuestion);
    }

    public AiToolAnswer maintenanceByProperty(String userQuestion) {
        return super.maintenanceByProperty(userQuestion);
    }

    public AiToolAnswer maintenanceByCategoryOrType(String userQuestion) {
        return super.maintenanceByCategoryOrType(userQuestion);
    }

    public AiToolAnswer maintenanceCostSummary(String userQuestion) {
        return super.maintenanceCostSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCostByProperty() {
        return super.maintenanceCostByProperty();
    }

    public AiToolAnswer maintenanceCostByCategory() {
        return super.maintenanceCostByCategory();
    }

    public AiToolAnswer maintenanceCostByMonth() {
        return super.maintenanceCostByMonth();
    }

    public AiToolAnswer maintenanceImagesSummary(boolean withoutImages) {
        return super.maintenanceImagesSummary(withoutImages);
    }

}
