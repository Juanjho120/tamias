package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PropertyCatalogReadOnlyToolService extends AiReadOnlyToolSupport {

    public PropertyCatalogReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer searchProperties(String userQuestion) {
        return super.searchProperties(userQuestion);
    }

    public AiToolAnswer activeProperties() {
        return super.activeProperties();
    }

    public AiToolAnswer inactiveProperties() {
        return super.inactiveProperties();
    }

    public AiToolAnswer propertySummary(String userQuestion) {
        return super.propertySummary(userQuestion);
    }

    public AiToolAnswer propertyOperationalOverview() {
        return super.propertyOperationalOverview();
    }

    public AiToolAnswer propertyImagesSummary(String userQuestion) {
        return super.propertyImagesSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCategories() {
        return super.maintenanceCategories();
    }

    public AiToolAnswer maintenanceTypes() {
        return super.maintenanceTypes();
    }

    public AiToolAnswer maintenanceCatalogOverview() {
        return super.maintenanceCatalogOverview();
    }

    public AiToolAnswer reservationPlatforms() {
        return super.reservationPlatforms();
    }

    public AiToolAnswer taskCategories() {
        return super.taskCategories();
    }

    public AiToolAnswer purchaseCategories() {
        return super.purchaseCategories();
    }

    public AiToolAnswer inventoryItemTypes() {
        return super.inventoryItemTypes();
    }

    public AiToolAnswer catalogSearch(String userQuestion) {
        return super.catalogSearch(userQuestion);
    }

}
