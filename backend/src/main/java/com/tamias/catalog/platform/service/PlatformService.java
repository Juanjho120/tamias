package com.tamias.catalog.platform.service;

import com.tamias.catalog.platform.entity.Platform;
import com.tamias.catalog.platform.repository.PlatformRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.service.BaseCatalogService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class PlatformService extends BaseCatalogService<Platform> {

    public PlatformService(
            PlatformRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            CatalogMapper catalogMapper
    ) {
        super(repository, organizationRepository, currentUserService, catalogMapper);
    }

    @Override
    protected Platform newEntity() {
        return new Platform();
    }

    @Override
    protected String getCatalogName() {
        return "platform";
    }
}
