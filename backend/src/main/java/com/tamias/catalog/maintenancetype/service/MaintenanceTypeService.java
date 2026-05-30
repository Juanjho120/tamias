package com.tamias.catalog.maintenancetype.service;

import com.tamias.catalog.dto.MaintenanceTypeRequest;
import com.tamias.catalog.dto.MaintenanceTypeResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenancetype.entity.MaintenanceType;
import com.tamias.catalog.maintenancetype.repository.MaintenanceTypeRepository;
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
public class MaintenanceTypeService {

    private final MaintenanceTypeRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public MaintenanceTypeService(
            MaintenanceTypeRepository repository,
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
    public PageResponse<MaintenanceTypeResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<MaintenanceType> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toMaintenanceTypeResponse));
    }

    @Transactional(readOnly = true)
    public MaintenanceTypeResponse findById(UUID id) {
        return catalogMapper.toMaintenanceTypeResponse(findEntity(id));
    }

    @Transactional
    public MaintenanceTypeResponse create(MaintenanceTypeRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("maintenance type name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        MaintenanceType entity = new MaintenanceType();
        entity.setOrganization(organization);
        catalogMapper.updateMaintenanceType(entity, request);

        return catalogMapper.toMaintenanceTypeResponse(repository.save(entity));
    }

    @Transactional
    public MaintenanceTypeResponse update(UUID id, MaintenanceTypeRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceType entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("maintenance type name already exists");
        }

        catalogMapper.updateMaintenanceType(entity, request);

        return catalogMapper.toMaintenanceTypeResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        MaintenanceType entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private MaintenanceType findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("maintenance type not found"));
    }
}
