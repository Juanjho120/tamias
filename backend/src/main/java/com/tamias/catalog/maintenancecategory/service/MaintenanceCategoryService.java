package com.tamias.catalog.maintenancecategory.service;

import com.tamias.catalog.maintenancecategory.entity.MaintenanceCategory;
import com.tamias.catalog.maintenancecategory.repository.MaintenanceCategoryRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class MaintenanceCategoryService extends BaseCatalogService<MaintenanceCategory> {

    public MaintenanceCategoryService(
            MaintenanceCategoryRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, catalogMapper);
    }

    @Override
    protected MaintenanceCategory newEntity() {
        return new MaintenanceCategory();
    }

    @Override
    protected String getCatalogName() {
        return "maintenance category";
    }
}
