package com.tamias.catalog.material.service;

import com.tamias.catalog.dto.MaterialRequest;
import com.tamias.catalog.dto.MaterialResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.material.entity.Material;
import com.tamias.catalog.material.repository.MaterialRepository;
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
public class MaterialService {

    private final MaterialRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public MaterialService(
            MaterialRepository repository,
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
    public PageResponse<MaterialResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<Material> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toMaterialResponse));
    }

    @Transactional(readOnly = true)
    public MaterialResponse findById(UUID id) {
        return catalogMapper.toMaterialResponse(findEntity(id));
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("material name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        Material entity = new Material();
        entity.setOrganization(organization);
        catalogMapper.updateMaterial(entity, request);

        return catalogMapper.toMaterialResponse(repository.save(entity));
    }

    @Transactional
    public MaterialResponse update(UUID id, MaterialRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Material entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("material name already exists");
        }

        catalogMapper.updateMaterial(entity, request);

        return catalogMapper.toMaterialResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        Material entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private Material findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("material not found"));
    }
}
