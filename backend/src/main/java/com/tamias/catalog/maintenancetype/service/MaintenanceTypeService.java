package com.tamias.catalog.maintenancetype.service;

import com.tamias.catalog.dto.MaintenanceTypeRequest;
import com.tamias.catalog.dto.MaintenanceTypeResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.maintenancetype.entity.MaintenanceType;
import com.tamias.catalog.maintenancetype.repository.MaintenanceTypeRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceTypeService {

    private final MaintenanceTypeRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CatalogMapper catalogMapper;

    public MaintenanceTypeService(
            MaintenanceTypeRepository repository,
            OrganizationRepository organizationRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository,
            CatalogMapper catalogMapper
    ) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.catalogMapper = catalogMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<MaintenanceTypeResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Page<MaintenanceType> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toMaintenanceTypeResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MaintenanceTypeResponse findById(UUID id) {
        return catalogMapper.toMaintenanceTypeResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public MaintenanceTypeResponse create(MaintenanceTypeRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedName = request.name().trim();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("maintenance type name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = findCurrentUser();

        MaintenanceType entity = new MaintenanceType();
        entity.setOrganization(organization);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        catalogMapper.updateMaintenanceType(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toMaintenanceTypeResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public MaintenanceTypeResponse update(UUID id, MaintenanceTypeRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        MaintenanceType entity = findEntity(id);
        String normalizedName = request.name().trim();

        if (!entity.getName().equalsIgnoreCase(normalizedName)
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("maintenance type name already exists");
        }

        catalogMapper.updateMaintenanceType(entity, request);
        entity.setName(normalizedName);
        entity.setUpdatedBy(findCurrentUser());

        return catalogMapper.toMaintenanceTypeResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        MaintenanceType entity = findEntity(id);
        User currentUser = findCurrentUser();

        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        repository.save(entity);
    }

    private MaintenanceType findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("maintenance type not found"));
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateWritableStatus(CatalogStatus status) {
        if (status == CatalogStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete maintenance type");
        }
    }
}
