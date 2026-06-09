package com.tamias.maintenance.detail.service;

import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
import com.tamias.catalog.material.entity.Material;
import com.tamias.catalog.material.repository.MaterialRepository;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedRequest;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedResponse;
import com.tamias.maintenance.detail.dto.MaintenanceMaterialUsedUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.entity.MaintenanceMaterialUsed;
import com.tamias.maintenance.detail.entity.MaintenanceRecordPerson;
import com.tamias.maintenance.detail.mapper.MaintenanceDetailMapper;
import com.tamias.maintenance.detail.repository.MaintenanceMaterialUsedRepository;
import com.tamias.maintenance.detail.repository.MaintenanceRecordPersonRepository;
import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.maintenance.repository.MaintenanceRecordRepository;
import com.tamias.security.service.CurrentUserService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceDetailService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final MaintenancePersonRepository maintenancePersonRepository;
    private final MaintenanceRecordPersonRepository maintenanceRecordPersonRepository;
    private final MaterialRepository materialRepository;
    private final MaintenanceMaterialUsedRepository maintenanceMaterialUsedRepository;
    private final CurrentUserService currentUserService;
    private final MaintenanceDetailMapper maintenanceDetailMapper;

    public MaintenanceDetailService(
            MaintenanceRecordRepository maintenanceRecordRepository,
            MaintenancePersonRepository maintenancePersonRepository,
            MaintenanceRecordPersonRepository maintenanceRecordPersonRepository,
            MaterialRepository materialRepository,
            MaintenanceMaterialUsedRepository maintenanceMaterialUsedRepository,
            CurrentUserService currentUserService,
            MaintenanceDetailMapper maintenanceDetailMapper
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenancePersonRepository = maintenancePersonRepository;
        this.maintenanceRecordPersonRepository = maintenanceRecordPersonRepository;
        this.materialRepository = materialRepository;
        this.maintenanceMaterialUsedRepository = maintenanceMaterialUsedRepository;
        this.currentUserService = currentUserService;
        this.maintenanceDetailMapper = maintenanceDetailMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<MaintenanceRecordPersonResponse> findPeople(UUID maintenanceRecordId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        return maintenanceRecordPersonRepository
                .findByMaintenanceRecord_IdAndOrganization_Id(maintenanceRecordId, organizationId)
                .stream()
                .map(maintenanceDetailMapper::toPersonResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceRecordPersonResponse addPerson(
            UUID maintenanceRecordId,
            MaintenanceRecordPersonRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceRecord maintenanceRecord = validateMaintenanceRecord(maintenanceRecordId, organizationId);
        MaintenancePerson maintenancePerson = maintenancePersonRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenancePersonId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance person not found"));

        if (maintenanceRecordPersonRepository.existsByMaintenanceRecord_IdAndMaintenancePerson_IdAndOrganization_Id(
                maintenanceRecordId,
                request.maintenancePersonId(),
                organizationId
        )) {
            throw new ConflictException("Maintenance person is already assigned to this maintenance record");
        }

        MaintenanceRecordPerson entity = new MaintenanceRecordPerson();
        entity.setOrganization(maintenanceRecord.getOrganization());
        entity.setMaintenanceRecord(maintenanceRecord);
        entity.setMaintenancePerson(maintenancePerson);

        return maintenanceDetailMapper.toPersonResponse(maintenanceRecordPersonRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void removePerson(UUID maintenanceRecordId, UUID personAssignmentId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceRecordPerson entity = maintenanceRecordPersonRepository
                .findByIdAndMaintenanceRecord_IdAndOrganization_Id(
                        personAssignmentId,
                        maintenanceRecordId,
                        organizationId
                )
                .orElseThrow(() -> new NotFoundException("Maintenance record person assignment not found"));

        maintenanceRecordPersonRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<MaintenanceMaterialUsedResponse> findMaterials(UUID maintenanceRecordId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        return maintenanceMaterialUsedRepository
                .findByMaintenanceRecord_IdAndOrganization_IdOrderByIdAsc(maintenanceRecordId, organizationId)
                .stream()
                .map(maintenanceDetailMapper::toMaterialUsedResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceMaterialUsedResponse addMaterial(
            UUID maintenanceRecordId,
            MaintenanceMaterialUsedRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceRecord maintenanceRecord = validateMaintenanceRecord(maintenanceRecordId, organizationId);
        Material material = resolveMaterial(request.materialId(), organizationId);
        String materialNameSnapshot = resolveMaterialNameSnapshot(request.materialNameSnapshot(), material);
        String unit = resolveUnit(request.unit(), material);

        MaintenanceMaterialUsed entity = new MaintenanceMaterialUsed();
        entity.setOrganization(maintenanceRecord.getOrganization());
        entity.setMaintenanceRecord(maintenanceRecord);

        maintenanceDetailMapper.updateMaterialUsed(entity, request, material, materialNameSnapshot, unit);

        return maintenanceDetailMapper.toMaterialUsedResponse(maintenanceMaterialUsedRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceMaterialUsedResponse updateMaterial(
            UUID maintenanceRecordId,
            UUID materialUsedId,
            MaintenanceMaterialUsedUpdateRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceMaterialUsed entity = maintenanceMaterialUsedRepository
                .findByIdAndMaintenanceRecord_IdAndOrganization_Id(
                        materialUsedId,
                        maintenanceRecordId,
                        organizationId
                )
                .orElseThrow(() -> new NotFoundException("Maintenance material used not found"));

        Material material = resolveMaterial(request.materialId(), organizationId);
        String materialNameSnapshot = resolveMaterialNameSnapshot(request.materialNameSnapshot(), material);
        String unit = resolveUnit(request.unit(), material);

        maintenanceDetailMapper.updateMaterialUsed(entity, request, material, materialNameSnapshot, unit);

        return maintenanceDetailMapper.toMaterialUsedResponse(maintenanceMaterialUsedRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void removeMaterial(UUID maintenanceRecordId, UUID materialUsedId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceMaterialUsed entity = maintenanceMaterialUsedRepository
                .findByIdAndMaintenanceRecord_IdAndOrganization_Id(
                        materialUsedId,
                        maintenanceRecordId,
                        organizationId
                )
                .orElseThrow(() -> new NotFoundException("Maintenance material used not found"));

        maintenanceMaterialUsedRepository.delete(entity);
    }

    private MaintenanceRecord validateMaintenanceRecord(UUID maintenanceRecordId, UUID organizationId) {
        return maintenanceRecordRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(maintenanceRecordId, organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
    }

    private Material resolveMaterial(UUID materialId, UUID organizationId) {
        if (materialId == null) {
            return null;
        }

        return materialRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(materialId, organizationId)
                .orElseThrow(() -> new NotFoundException("Material not found"));
    }

    private String resolveMaterialNameSnapshot(String requestedName, Material material) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        if (material != null) {
            return material.getName();
        }

        throw new BadRequestException("Material name is required when materialId is not provided");
    }

    private String resolveUnit(String requestedUnit, Material material) {
        if (requestedUnit != null && !requestedUnit.isBlank()) {
            return requestedUnit.trim();
        }

        if (material != null) {
            return material.getUnit();
        }

        return null;
    }
}
