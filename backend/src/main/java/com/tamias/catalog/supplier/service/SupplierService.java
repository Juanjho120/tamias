package com.tamias.catalog.supplier.service;

import com.tamias.catalog.dto.SupplierRequest;
import com.tamias.catalog.dto.SupplierResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.supplier.entity.Supplier;
import com.tamias.catalog.supplier.repository.SupplierRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
public class SupplierService {

    private final SupplierRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public SupplierService(
            SupplierRepository repository,
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
    public PageResponse<SupplierResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<Supplier> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toSupplierResponse));
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(UUID id) {
        return catalogMapper.toSupplierResponse(findEntity(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("supplier name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        Supplier entity = new Supplier();
        entity.setOrganization(organization);
        catalogMapper.updateSupplier(entity, request);

        return catalogMapper.toSupplierResponse(repository.save(entity));
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Supplier entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("supplier name already exists");
        }

        catalogMapper.updateSupplier(entity, request);

        return catalogMapper.toSupplierResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        Supplier entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private Supplier findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("supplier not found"));
    }
}
