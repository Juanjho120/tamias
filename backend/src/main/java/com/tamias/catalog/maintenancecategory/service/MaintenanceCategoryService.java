package com.tamias.catalog.maintenancecategory.service;

import com.tamias.catalog.maintenancecategory.entity.MaintenanceCategory;
import com.tamias.catalog.maintenancecategory.repository.MaintenanceCategoryRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class MaintenanceCategoryService extends BaseCatalogService<MaintenanceCategory> {

    public MaintenanceCategoryService(
            MaintenanceCategoryRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, userRepository, catalogMapper);
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
