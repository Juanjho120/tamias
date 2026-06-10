package com.tamias.property.service;

import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.dto.PropertyRequest;
import com.tamias.property.dto.PropertyResponse;
import com.tamias.property.dto.PropertySummaryResponse;
import com.tamias.property.entity.Property;
import com.tamias.property.enums.PropertyStatus;
import com.tamias.property.mapper.PropertyMapper;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {
    private final PropertyRepository propertyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PropertyMapper propertyMapper;
    private final CurrentUserService currentUserService;

    public PropertyService(
        PropertyRepository propertyRepository,
        OrganizationRepository organizationRepository,
        UserRepository userRepository,
        PropertyMapper propertyMapper,
        CurrentUserService currentUserService
    ) {
        this.propertyRepository = propertyRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.propertyMapper = propertyMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<PropertySummaryResponse> findAll(
        PropertyStatus status,
        String search,
        Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = normalizeSearch(search);

        Page<Property> page;

        if (status == null && normalizedSearch == null) {
            page = propertyRepository.findByOrganization_IdAndDeletedAtIsNull(
                organizationId,
                pageable
            );
        } else if (status != null && normalizedSearch == null) {
            page = propertyRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                status,
                pageable
            );
        } else if (status == null) {
            page = propertyRepository.searchByText(
                organizationId,
                normalizedSearch,
                pageable
            );
        } else {
            page = propertyRepository.searchByStatusAndText(
                organizationId,
                status,
                normalizedSearch,
                pageable
            );
        }

        return PageResponse.from(page.map(propertyMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PropertyResponse findById(UUID id) {
        Property property = findPropertyInCurrentOrganization(id);
        return propertyMapper.toResponse(property);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public PropertyResponse create(PropertyRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedName = request.name().trim();

        if (propertyRepository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(
            organizationId,
            normalizedName
        )) {
            throw new ConflictException("Property name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        Property property = propertyMapper.toEntity(request);
        property.setName(normalizedName);
        property.setOrganization(organization);
        property.setCreatedBy(currentUser);
        property.setUpdatedBy(currentUser);

        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public PropertyResponse update(UUID id, PropertyRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Property property = findPropertyInCurrentOrganization(id);
        String normalizedName = request.name().trim();

        if (!property.getName().equalsIgnoreCase(normalizedName)
            && propertyRepository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(
                organizationId,
                normalizedName
            )) {
            throw new ConflictException("Property name already exists");
        }

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        propertyMapper.updateEntity(property, request);
        property.setName(normalizedName);
        property.setUpdatedBy(currentUser);

        return propertyMapper.toResponse(propertyRepository.save(property));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void delete(UUID id) {
        Property property = findPropertyInCurrentOrganization(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        property.setStatus(PropertyStatus.DELETED);
        property.setDeletedAt(OffsetDateTime.now());
        property.setDeletedBy(currentUser);
        property.setUpdatedBy(currentUser);

        propertyRepository.save(property);
    }

    private Property findPropertyInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return propertyRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Property not found"));
    }

    private void validateWritableStatus(PropertyStatus status) {
        if (status == PropertyStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete a property");
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }

        return search.trim();
    }
}
