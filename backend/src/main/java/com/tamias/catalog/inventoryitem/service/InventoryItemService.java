package com.tamias.catalog.inventoryitem.service;

import com.tamias.catalog.dto.InventoryItemRequest;
import com.tamias.catalog.dto.InventoryItemResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
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
public class InventoryItemService {

    private final InventoryItemRepository repository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final CatalogMapper catalogMapper;

    public InventoryItemService(
            InventoryItemRepository repository,
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<InventoryItemResponse> findAll(
            CatalogStatus status,
            InventoryItemType itemType,
            Boolean availableForMaintenance,
            Boolean availableForReservations,
            Boolean availableForPurchases,
            String search,
            Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        Page<InventoryItem> page = repository.search(
                organizationId,
                status,
                itemType,
                availableForMaintenance,
                availableForReservations,
                availableForPurchases,
                normalizedSearch,
                pageable
        );

        return PageResponse.from(page.map(catalogMapper::toInventoryItemResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public InventoryItemResponse findById(UUID id) {
        return catalogMapper.toInventoryItemResponse(findEntity(id));
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("inventory item name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        InventoryItem entity = new InventoryItem();
        entity.setOrganization(organization);
        catalogMapper.updateInventoryItem(entity, request);

        return catalogMapper.toInventoryItemResponse(repository.save(entity));
    }

    @Transactional
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        InventoryItem entity = findEntity(id);

        if (!entity.getName().equalsIgnoreCase(request.name())
                && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, request.name())) {
            throw new ConflictException("inventory item name already exists");
        }

        catalogMapper.updateInventoryItem(entity, request);

        return catalogMapper.toInventoryItemResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        InventoryItem entity = findEntity(id);
        entity.setStatus(CatalogStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    public InventoryItem findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return repository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("inventory item not found"));
    }
}
