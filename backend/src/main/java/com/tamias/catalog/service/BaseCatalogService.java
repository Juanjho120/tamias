package com.tamias.catalog.service;

import com.tamias.catalog.dto.CatalogRequest;
import com.tamias.catalog.dto.CatalogResponse;
import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.repository.BaseCatalogRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseCatalogService<T extends BaseCatalogEntity> {

    private final BaseCatalogRepository<T> repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    protected BaseCatalogService(
            BaseCatalogRepository<T> repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            CatalogMapper catalogMapper
    ) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.currentUserService = currentUserService;
        this.catalogMapper = catalogMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CatalogResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<T> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toCatalogResponse));
    }

    @Transactional(readOnly = true)
    public CatalogResponse findById(UUID id) {
        return catalogMapper.toCatalogResponse(findEntityInCurrentOrganization(id));
    }

    @Transactional
    public CatalogResponse create(CatalogRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException(getCatalogName() + " name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        T entity = newEntity();
        entity.setOrganization(organization);
        catalogMapper.updateBaseCatalog(entity, request);

        return catalogMapper.toCatalogResponse(repository.save(entity));
    }

    @Transactional
    public CatalogResponse update(UUID id, CatalogRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        T entity = findEntityInCurrentOrganization(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException(getCatalogName() + " name already exists");
        }

        catalogMapper.updateBaseCatalog(entity, request);

        return catalogMapper.toCatalogResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        T entity = findEntityInCurrentOrganization(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    protected T findEntityInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException(getCatalogName() + " not found"));
    }

    protected abstract T newEntity();

    protected abstract String getCatalogName();
}
