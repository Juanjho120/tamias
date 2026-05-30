package com.tamias.catalog.maintenanceperson.service;

import com.tamias.catalog.dto.MaintenancePersonRequest;
import com.tamias.catalog.dto.MaintenancePersonResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
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
public class MaintenancePersonService {

    private final MaintenancePersonRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public MaintenancePersonService(
            MaintenancePersonRepository repository,
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
    public PageResponse<MaintenancePersonResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<MaintenancePerson> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toMaintenancePersonResponse));
    }

    @Transactional(readOnly = true)
    public MaintenancePersonResponse findById(UUID id) {
        return catalogMapper.toMaintenancePersonResponse(findEntity(id));
    }

    @Transactional
    public MaintenancePersonResponse create(MaintenancePersonRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("maintenance person name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        MaintenancePerson entity = new MaintenancePerson();
        entity.setOrganization(organization);
        catalogMapper.updateMaintenancePerson(entity, request);

        return catalogMapper.toMaintenancePersonResponse(repository.save(entity));
    }

    @Transactional
    public MaintenancePersonResponse update(UUID id, MaintenancePersonRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        MaintenancePerson entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("maintenance person name already exists");
        }

        catalogMapper.updateMaintenancePerson(entity, request);

        return catalogMapper.toMaintenancePersonResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        MaintenancePerson entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    private MaintenancePerson findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("maintenance person not found"));
    }
}
