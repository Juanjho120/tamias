package com.tamias.catalog.supplier.service;

import com.tamias.catalog.dto.SupplierRequest;
import com.tamias.catalog.dto.SupplierResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.catalog.supplier.entity.Supplier;
import com.tamias.catalog.supplier.repository.SupplierRepository;
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
public class SupplierService {

    private final SupplierRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final CatalogMapper catalogMapper;

    public SupplierService(
            SupplierRepository repository,
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
    public PageResponse<SupplierResponse> findAll(CatalogStatus status, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Page<Supplier> page = status == null
                ? repository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : repository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable);

        return PageResponse.from(page.map(catalogMapper::toSupplierResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public SupplierResponse findById(UUID id) {
        return catalogMapper.toSupplierResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public SupplierResponse create(SupplierRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedName = request.name().trim();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("supplier name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = findCurrentUser();

        Supplier entity = new Supplier();
        entity.setOrganization(organization);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        catalogMapper.updateSupplier(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toSupplierResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public SupplierResponse update(UUID id, SupplierRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Supplier entity = findEntity(id);
        String normalizedName = request.name().trim();

        if (!entity.getName().equalsIgnoreCase(normalizedName)
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("supplier name already exists");
        }

        catalogMapper.updateSupplier(entity, request);
        entity.setName(normalizedName);
        entity.setUpdatedBy(findCurrentUser());

        return catalogMapper.toSupplierResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        Supplier entity = findEntity(id);
        User currentUser = findCurrentUser();

        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        repository.save(entity);
    }

    private Supplier findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("supplier not found"));
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void validateWritableStatus(CatalogStatus status) {
        if (status == CatalogStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete supplier");
        }
    }
}
