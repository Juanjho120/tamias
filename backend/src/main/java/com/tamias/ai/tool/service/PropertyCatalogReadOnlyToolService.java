package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.PropertyCatalogToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyCatalogReadOnlyToolService {

    private final PropertyCatalogToolRepository repository;

    public PropertyCatalogReadOnlyToolService(PropertyCatalogToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer searchProperties(String userQuestion) {
        return repository.searchProperties(userQuestion);
    }

    public AiToolAnswer activeProperties() {
        return repository.activeProperties();
    }

    public AiToolAnswer inactiveProperties() {
        return repository.inactiveProperties();
    }

    public AiToolAnswer propertySummary(String userQuestion) {
        return repository.propertySummary(userQuestion);
    }

    public AiToolAnswer propertyOperationalOverview() {
        return repository.propertyOperationalOverview();
    }

    public AiToolAnswer propertyImagesSummary(String userQuestion) {
        return repository.propertyImagesSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCategories() {
        return repository.maintenanceCategories();
    }

    public AiToolAnswer maintenanceTypes() {
        return repository.maintenanceTypes();
    }

    public AiToolAnswer maintenanceCatalogOverview() {
        return repository.maintenanceCatalogOverview();
    }

    public AiToolAnswer reservationPlatforms() {
        return repository.reservationPlatforms();
    }

    public AiToolAnswer taskCategories() {
        return repository.taskCategories();
    }

    public AiToolAnswer purchaseCategories() {
        return repository.purchaseCategories();
    }

    public AiToolAnswer inventoryItemTypes() {
        return repository.inventoryItemTypes();
    }

    public AiToolAnswer catalogSearch(String userQuestion) {
        return repository.catalogSearch(userQuestion);
    }
}
