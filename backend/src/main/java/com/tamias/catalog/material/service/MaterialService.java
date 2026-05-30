package com.tamias.catalog.material.service;

import com.tamias.catalog.material.entity.Material;
import com.tamias.catalog.material.repository.MaterialRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class MaterialService extends BaseCatalogService<Material> {

    public MaterialService(
            MaterialRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, catalogMapper);
    }

    @Override
    protected Material newEntity() {
        return new Material();
    }

    @Override
    protected String getCatalogName() {
        return "material";
    }
}
