package com.tamias.maintenance.service;

import com.tamias.catalog.maintenancecategory.repository.MaintenanceCategoryRepository;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
import com.tamias.catalog.maintenancetype.repository.MaintenanceTypeRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.maintenance.dto.MaintenanceRecordRequest;
import com.tamias.maintenance.dto.MaintenanceRecordResponse;
import com.tamias.maintenance.dto.MaintenanceRecordSummaryResponse;
import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.maintenance.enums.MaintenanceStatus;
import com.tamias.maintenance.mapper.MaintenanceRecordMapper;
import com.tamias.maintenance.repository.MaintenanceRecordRepository;
import com.tamias.organization.repository.OrganizationRepository;
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
public class MaintenanceRecordService {
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final MaintenanceCategoryRepository maintenanceCategoryRepository;
    private final MaintenanceTypeRepository maintenanceTypeRepository;
    private final MaintenancePersonRepository maintenancePersonRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final MaintenanceRecordMapper maintenanceRecordMapper;

    public MaintenanceRecordService(
        MaintenanceRecordRepository maintenanceRecordRepository,
        OrganizationRepository organizationRepository,
        PropertyRepository propertyRepository,
        MaintenanceCategoryRepository maintenanceCategoryRepository,
        MaintenanceTypeRepository maintenanceTypeRepository,
        MaintenancePersonRepository maintenancePersonRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        MaintenanceRecordMapper maintenanceRecordMapper
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.maintenanceCategoryRepository = maintenanceCategoryRepository;
        this.maintenanceTypeRepository = maintenanceTypeRepository;
        this.maintenancePersonRepository = maintenancePersonRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.maintenanceRecordMapper = maintenanceRecordMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<MaintenanceRecordSummaryResponse> findAll(
        UUID propertyId,
        MaintenanceStatus status,
        Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Page<MaintenanceRecord> page;

        if (propertyId == null && status == null) {
            page = maintenanceRecordRepository.findByOrganization_IdAndDeletedAtIsNull(
                organizationId,
                pageable
            );
        } else if (propertyId != null && status == null) {
            page = maintenanceRecordRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
                organizationId,
                propertyId,
                pageable
            );
        } else if (propertyId == null) {
            page = maintenanceRecordRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                status,
                pageable
            );
        } else {
            page = maintenanceRecordRepository.findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                propertyId,
                status,
                pageable
            );
        }

        return PageResponse.from(page.map(maintenanceRecordMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MaintenanceRecordResponse findById(UUID id) {
        return maintenanceRecordMapper.toResponse(findEntityInCurrentOrganization(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceRecordResponse create(MaintenanceRecordRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        var property = propertyRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
            .orElseThrow(() -> new NotFoundException("Property not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        MaintenanceRecord entity = new MaintenanceRecord();
        entity.setOrganization(organization);
        entity.setProperty(property);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        maintenanceRecordMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);

        return maintenanceRecordMapper.toResponse(maintenanceRecordRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceRecordResponse update(UUID id, MaintenanceRecordRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        MaintenanceRecord entity = findEntityInCurrentOrganization(id);

        var property = propertyRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
            .orElseThrow(() -> new NotFoundException("Property not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        entity.setProperty(property);
        entity.setUpdatedBy(currentUser);

        maintenanceRecordMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);

        return maintenanceRecordMapper.toResponse(maintenanceRecordRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        MaintenanceRecord entity = findEntityInCurrentOrganization(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        entity.setStatus(MaintenanceStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        maintenanceRecordRepository.save(entity);
    }

    private MaintenanceRecord findEntityInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return maintenanceRecordRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
    }

    private void setOptionalRelations(
        MaintenanceRecord entity,
        MaintenanceRecordRequest request,
        UUID organizationId
    ) {
        if (request.maintenanceCategoryId() == null) {
            entity.setMaintenanceCategory(null);
        } else {
            var maintenanceCategory = maintenanceCategoryRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenanceCategoryId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance category not found"));

            entity.setMaintenanceCategory(maintenanceCategory);
        }

        if (request.maintenanceTypeId() == null) {
            entity.setMaintenanceType(null);
        } else {
            var maintenanceType = maintenanceTypeRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenanceTypeId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance type not found"));

            entity.setMaintenanceType(maintenanceType);
        }

        if (request.maintenancePersonId() == null) {
            entity.setMaintenancePerson(null);
        } else {
            var maintenancePerson = maintenancePersonRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenancePersonId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance person not found"));

            entity.setMaintenancePerson(maintenancePerson);
        }
    }

    private void validateWritableStatus(MaintenanceStatus status) {
        if (status == MaintenanceStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete a maintenance record");
        }
    }
}
