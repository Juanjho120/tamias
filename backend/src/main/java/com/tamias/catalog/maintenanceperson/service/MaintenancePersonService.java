package com.tamias.catalog.maintenanceperson.service;

import com.tamias.catalog.dto.MaintenancePersonRequest;
import com.tamias.catalog.dto.MaintenancePersonResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
import com.tamias.catalog.mapper.CatalogMapper;
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
public class MaintenancePersonService {

    private final MaintenancePersonRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CatalogMapper catalogMapper;

    public MaintenancePersonService(
            MaintenancePersonRepository repository,
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
    public PageResponse<MaintenancePersonResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Page<MaintenancePerson> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toMaintenancePersonResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MaintenancePersonResponse findById(UUID id) {
        return catalogMapper.toMaintenancePersonResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public MaintenancePersonResponse create(MaintenancePersonRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedFullName = request.fullName().trim();

        if (repository.existsByOrganization_IdAndFullNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedFullName)) {
            throw new ConflictException("maintenance person full name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = findCurrentUser();

        MaintenancePerson entity = new MaintenancePerson();
        entity.setOrganization(organization);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        catalogMapper.updateMaintenancePerson(entity, request);
        entity.setFullName(normalizedFullName);

        return catalogMapper.toMaintenancePersonResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public MaintenancePersonResponse update(UUID id, MaintenancePersonRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        MaintenancePerson entity = findEntity(id);
        String normalizedFullName = request.fullName().trim();

        if (!entity.getFullName().equalsIgnoreCase(normalizedFullName)
                && repository.existsByOrganization_IdAndFullNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedFullName)) {
            throw new ConflictException("maintenance person full name already exists");
        }

        catalogMapper.updateMaintenancePerson(entity, request);
        entity.setFullName(normalizedFullName);
        entity.setUpdatedBy(findCurrentUser());

        return catalogMapper.toMaintenancePersonResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        MaintenancePerson entity = findEntity(id);
        User currentUser = findCurrentUser();

        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        repository.save(entity);
    }

    private MaintenancePerson findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("maintenance person not found"));
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateWritableStatus(CatalogStatus status) {
        if (status == CatalogStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete maintenance person");
        }
    }
}
