package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.MaintenanceToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaintenanceReadOnlyToolService {

    private final MaintenanceToolRepository repository;

    public MaintenanceReadOnlyToolService(MaintenanceToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer lastPerformedMaintenance(String userQuestion) {
        return repository.lastPerformedMaintenance(userQuestion);
    }

    public AiToolAnswer maintenanceSearch(String userQuestion) {
        return repository.maintenanceSearch(userQuestion);
    }

    public AiToolAnswer recentMaintenance() {
        return repository.recentMaintenance();
    }

    public AiToolAnswer maintenanceByStatus(String userQuestion) {
        return repository.maintenanceByStatus(userQuestion);
    }

    public AiToolAnswer maintenanceByProperty(String userQuestion) {
        return repository.maintenanceByProperty(userQuestion);
    }

    public AiToolAnswer maintenanceByCategoryOrType(String userQuestion) {
        return repository.maintenanceByCategoryOrType(userQuestion);
    }

    public AiToolAnswer maintenanceCostSummary(String userQuestion) {
        return repository.maintenanceCostSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCostByProperty() {
        return repository.maintenanceCostByProperty();
    }

    public AiToolAnswer maintenanceCostByCategory() {
        return repository.maintenanceCostByCategory();
    }

    public AiToolAnswer maintenanceCostByMonth() {
        return repository.maintenanceCostByMonth();
    }

    public AiToolAnswer maintenanceImagesSummary(boolean withoutImages) {
        return repository.maintenanceImagesSummary(withoutImages);
    }
}
