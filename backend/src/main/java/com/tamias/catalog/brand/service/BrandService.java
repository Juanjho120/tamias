package com.tamias.catalog.brand.service;

import com.tamias.catalog.brand.entity.Brand;
import com.tamias.catalog.brand.repository.BrandRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class BrandService extends BaseCatalogService<Brand> {

    public BrandService(
            BrandRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, catalogMapper);
    }

    @Override
    protected Brand newEntity() {
        return new Brand();
    }

    @Override
    protected String getCatalogName() {
        return "brand";
    }
}
