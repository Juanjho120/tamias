package com.tamias.maintenance.detail.service;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemResponse;
import com.tamias.maintenance.detail.dto.MaintenanceRecordItemUpdateRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonRequest;
import com.tamias.maintenance.detail.dto.MaintenanceRecordPersonResponse;
import com.tamias.maintenance.detail.entity.MaintenanceRecordItem;
import com.tamias.maintenance.detail.entity.MaintenanceRecordPerson;
import com.tamias.maintenance.detail.mapper.MaintenanceDetailMapper;
import com.tamias.maintenance.detail.repository.MaintenanceRecordItemRepository;
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
    private final InventoryItemRepository inventoryItemRepository;
    private final MaintenanceRecordItemRepository maintenanceRecordItemRepository;
    private final CurrentUserService currentUserService;
    private final MaintenanceDetailMapper maintenanceDetailMapper;

    public MaintenanceDetailService(
        MaintenanceRecordRepository maintenanceRecordRepository,
        MaintenancePersonRepository maintenancePersonRepository,
        MaintenanceRecordPersonRepository maintenanceRecordPersonRepository,
        InventoryItemRepository inventoryItemRepository,
        MaintenanceRecordItemRepository maintenanceRecordItemRepository,
        CurrentUserService currentUserService,
        MaintenanceDetailMapper maintenanceDetailMapper
    ) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenancePersonRepository = maintenancePersonRepository;
        this.maintenanceRecordPersonRepository = maintenanceRecordPersonRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.maintenanceRecordItemRepository = maintenanceRecordItemRepository;
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
    public MaintenanceRecordPersonResponse addPerson(UUID maintenanceRecordId, MaintenanceRecordPersonRequest request) {
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

        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        MaintenanceRecordPerson entity = maintenanceRecordPersonRepository
            .findByIdAndMaintenanceRecord_IdAndOrganization_Id(personAssignmentId, maintenanceRecordId, organizationId)
            .orElseThrow(() -> new NotFoundException("Maintenance record person assignment not found"));

        maintenanceRecordPersonRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<MaintenanceRecordItemResponse> findItems(UUID maintenanceRecordId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        return maintenanceRecordItemRepository
            .findByMaintenanceRecord_IdAndOrganization_IdOrderByIdAsc(maintenanceRecordId, organizationId)
            .stream()
            .map(maintenanceDetailMapper::toRecordItemResponse)
            .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceRecordItemResponse addItem(UUID maintenanceRecordId, MaintenanceRecordItemRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        MaintenanceRecord maintenanceRecord = validateMaintenanceRecord(maintenanceRecordId, organizationId);
        InventoryItem inventoryItem = resolveInventoryItem(request.requestedInventoryItemId(), organizationId);
        String itemNameSnapshot = resolveItemNameSnapshot(request.requestedItemNameSnapshot(), inventoryItem);
        String unit = resolveUnit(request.unit(), inventoryItem);

        MaintenanceRecordItem entity = new MaintenanceRecordItem();
        entity.setOrganization(maintenanceRecord.getOrganization());
        entity.setMaintenanceRecord(maintenanceRecord);

        maintenanceDetailMapper.updateRecordItem(entity, request, inventoryItem, itemNameSnapshot, unit);

        return maintenanceDetailMapper.toRecordItemResponse(maintenanceRecordItemRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public MaintenanceRecordItemResponse updateItem(
        UUID maintenanceRecordId,
        UUID itemId,
        MaintenanceRecordItemUpdateRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        MaintenanceRecordItem entity = maintenanceRecordItemRepository
            .findByIdAndMaintenanceRecord_IdAndOrganization_Id(itemId, maintenanceRecordId, organizationId)
            .orElseThrow(() -> new NotFoundException("Maintenance record item not found"));

        InventoryItem inventoryItem = resolveInventoryItem(request.requestedInventoryItemId(), organizationId);
        String itemNameSnapshot = resolveItemNameSnapshot(request.requestedItemNameSnapshot(), inventoryItem);
        String unit = resolveUnit(request.unit(), inventoryItem);

        maintenanceDetailMapper.updateRecordItem(entity, request, inventoryItem, itemNameSnapshot, unit);

        return maintenanceDetailMapper.toRecordItemResponse(maintenanceRecordItemRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void removeItem(UUID maintenanceRecordId, UUID itemId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        MaintenanceRecordItem entity = maintenanceRecordItemRepository
            .findByIdAndMaintenanceRecord_IdAndOrganization_Id(itemId, maintenanceRecordId, organizationId)
            .orElseThrow(() -> new NotFoundException("Maintenance record item not found"));

        maintenanceRecordItemRepository.delete(entity);
    }

    private MaintenanceRecord validateMaintenanceRecord(UUID maintenanceRecordId, UUID organizationId) {
        return maintenanceRecordRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(maintenanceRecordId, organizationId)
            .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
    }

    private InventoryItem resolveInventoryItem(UUID inventoryItemId, UUID organizationId) {
        if (inventoryItemId == null) {
            return null;
        }

        InventoryItem inventoryItem = inventoryItemRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(inventoryItemId, organizationId)
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        if (inventoryItem.getStatus() != CatalogStatus.ACTIVE || !Boolean.TRUE.equals(inventoryItem.getAvailableForMaintenance())) {
            throw new BadRequestException("Inventory item is not available for maintenance");
        }

        return inventoryItem;
    }

    private String resolveItemNameSnapshot(String requestedName, InventoryItem inventoryItem) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        if (inventoryItem != null) {
            return inventoryItem.getName();
        }

        throw new BadRequestException("Item name is required when inventoryItemId is not provided");
    }

    private String resolveUnit(String requestedUnit, InventoryItem inventoryItem) {
        if (requestedUnit != null && !requestedUnit.isBlank()) {
            return requestedUnit.trim();
        }

        if (inventoryItem != null) {
            return inventoryItem.getUnit();
        }

        return null;
    }
}
