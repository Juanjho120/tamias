package com.tamias.catalog.inventoryitem.service;

import com.tamias.catalog.dto.InventoryItemRequest;
import com.tamias.catalog.dto.InventoryItemResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
import com.tamias.catalog.mapper.CatalogMapper;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
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

        Page<InventoryItem> page = normalizedSearch == null
            ? repository.search(
                organizationId,
                status,
                itemType,
                availableForMaintenance,
                availableForReservations,
                availableForPurchases,
                pageable
            )
            : repository.searchWithText(
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public InventoryItemResponse create(InventoryItemRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String normalizedName = request.name().trim();

        if (repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("inventory item name already exists");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        InventoryItem entity = new InventoryItem();
        entity.setOrganization(organization);

        catalogMapper.updateInventoryItem(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toInventoryItemResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        InventoryItem entity = findEntity(id);
        String normalizedName = request.name().trim();

        if (!entity.getName().equalsIgnoreCase(normalizedName)
            && repository.existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(organizationId, normalizedName)) {
            throw new ConflictException("inventory item name already exists");
        }

        catalogMapper.updateInventoryItem(entity, request);
        entity.setName(normalizedName);

        return catalogMapper.toInventoryItemResponse(repository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
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

    private void validateWritableStatus(CatalogStatus status) {
        if (status == CatalogStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete inventory item");
        }
    }
}
