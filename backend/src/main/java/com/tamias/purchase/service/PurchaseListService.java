package com.tamias.purchase.service;

import com.tamias.catalog.brand.entity.Brand;
import com.tamias.catalog.brand.repository.BrandRepository;
import com.tamias.catalog.city.repository.CityRepository;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
import com.tamias.catalog.supplier.repository.SupplierRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.purchase.dto.PurchaseItemPurchasedRequest;
import com.tamias.purchase.dto.PurchaseItemRequest;
import com.tamias.purchase.dto.PurchaseItemResponse;
import com.tamias.purchase.dto.PurchaseItemUpdateRequest;
import com.tamias.purchase.dto.PurchaseListRequest;
import com.tamias.purchase.dto.PurchaseListResponse;
import com.tamias.purchase.dto.PurchaseListSummaryResponse;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.purchase.entity.PurchaseList;
import com.tamias.purchase.enums.PurchaseListStatus;
import com.tamias.purchase.mapper.PurchaseMapper;
import com.tamias.purchase.repository.PurchaseItemRepository;
import com.tamias.purchase.repository.PurchaseListRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseListService {

    private final PurchaseListRepository purchaseListRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final CityRepository cityRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PurchaseMapper purchaseMapper;

    public PurchaseListService(
            PurchaseListRepository purchaseListRepository,
            PurchaseItemRepository purchaseItemRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            CityRepository cityRepository,
            SupplierRepository supplierRepository,
            InventoryItemRepository inventoryItemRepository,
            BrandRepository brandRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            PurchaseMapper purchaseMapper
    ) {
        this.purchaseListRepository = purchaseListRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.cityRepository = cityRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.brandRepository = brandRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.purchaseMapper = purchaseMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<PurchaseListSummaryResponse> findAll(
            UUID propertyId,
            UUID supplierId,
            UUID cityId,
            PurchaseListStatus status,
            Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<PurchaseList> page;

        if (supplierId != null) {
            page = purchaseListRepository.findByOrganization_IdAndSupplier_IdAndDeletedAtIsNull(
                    organizationId,
                    supplierId,
                    pageable
            );
        } else if (cityId != null) {
            page = purchaseListRepository.findByOrganization_IdAndCity_IdAndDeletedAtIsNull(
                    organizationId,
                    cityId,
                    pageable
            );
        } else if (propertyId == null && status == null) {
            page = purchaseListRepository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable);
        } else if (propertyId != null && status == null) {
            page = purchaseListRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
                    organizationId,
                    propertyId,
                    pageable
            );
        } else if (propertyId == null) {
            page = purchaseListRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                    organizationId,
                    status,
                    pageable
            );
        } else {
            page = purchaseListRepository.findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
                    organizationId,
                    propertyId,
                    status,
                    pageable
            );
        }

        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PurchaseListResponse findById(UUID id) {
        PurchaseList purchaseList = findPurchaseList(id);

        return purchaseMapper.toResponse(
                purchaseList,
                purchaseItemRepository.findByPurchaseList_IdOrderByCreatedAtAsc(purchaseList.getId()),
                purchaseItemRepository.calculateEstimatedTotal(purchaseList.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PurchaseListResponse create(PurchaseListRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        PurchaseList purchaseList = new PurchaseList();
        purchaseList.setOrganization(organization);
        purchaseList.setCreatedBy(currentUser);
        purchaseList.setUpdatedBy(currentUser);

        purchaseMapper.updatePurchaseList(purchaseList, request);
        setOptionalRelations(purchaseList, request, organizationId);

        PurchaseList saved = purchaseListRepository.save(purchaseList);

        if (request.items() != null) {
            for (PurchaseItemRequest itemRequest : request.items()) {
                createItemEntity(saved, itemRequest, organizationId);
            }
        }

        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PurchaseListResponse update(UUID id, PurchaseListRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        PurchaseList purchaseList = findPurchaseList(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        purchaseList.setUpdatedBy(currentUser);

        purchaseMapper.updatePurchaseList(purchaseList, request);
        setOptionalRelations(purchaseList, request, organizationId);

        PurchaseList saved = purchaseListRepository.save(purchaseList);

        if (request.items() != null) {
            replaceItems(saved, request.items(), organizationId);
        }

        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        PurchaseList purchaseList = findPurchaseList(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        purchaseList.setStatus(PurchaseListStatus.DELETED);
        purchaseList.setDeletedAt(OffsetDateTime.now());
        purchaseList.setDeletedBy(currentUser);
        purchaseList.setUpdatedBy(currentUser);

        purchaseListRepository.save(purchaseList);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PurchaseItemResponse createItem(UUID purchaseListId, PurchaseItemRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        PurchaseList purchaseList = findPurchaseList(purchaseListId);
        PurchaseItem saved = createItemEntity(purchaseList, request, organizationId);

        return purchaseMapper.toItemResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PurchaseItemResponse updateItem(UUID purchaseListId, UUID itemId, PurchaseItemUpdateRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        PurchaseItem item = purchaseItemRepository
                .findByIdAndPurchaseList_IdAndOrganization_Id(itemId, purchaseListId, organizationId)
                .orElseThrow(() -> new NotFoundException("Purchase item not found"));

        InventoryItem inventoryItem = resolveInventoryItem(request.requestedInventoryItemId(), organizationId);
        Brand brand = resolveBrand(request.brandId(), organizationId);
        String itemNameSnapshot = resolveItemName(request.itemNameSnapshot(), inventoryItem);

        purchaseMapper.updatePurchaseItem(item, request, inventoryItem, brand, itemNameSnapshot);

        return purchaseMapper.toItemResponse(purchaseItemRepository.save(item));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public PurchaseItemResponse updateItemPurchased(
            UUID purchaseListId,
            UUID itemId,
            PurchaseItemPurchasedRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        PurchaseItem item = purchaseItemRepository
                .findByIdAndPurchaseList_IdAndOrganization_Id(itemId, purchaseListId, organizationId)
                .orElseThrow(() -> new NotFoundException("Purchase item not found"));

        item.setPurchased(request.purchased());

        return purchaseMapper.toItemResponse(purchaseItemRepository.save(item));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void deleteItem(UUID purchaseListId, UUID itemId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        PurchaseItem item = purchaseItemRepository
                .findByIdAndPurchaseList_IdAndOrganization_Id(itemId, purchaseListId, organizationId)
                .orElseThrow(() -> new NotFoundException("Purchase item not found"));

        purchaseItemRepository.delete(item);
    }

    private PurchaseList findPurchaseList(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return purchaseListRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Purchase list not found"));
    }

    private PurchaseListSummaryResponse toSummaryResponse(PurchaseList purchaseList) {
        return purchaseMapper.toSummaryResponse(
                purchaseList,
                purchaseItemRepository.countByPurchaseList_Id(purchaseList.getId()),
                purchaseItemRepository.countByPurchaseList_IdAndPurchased(purchaseList.getId(), true),
                purchaseItemRepository.calculateEstimatedTotal(purchaseList.getId())
        );
    }

    private PurchaseItem createItemEntity(PurchaseList purchaseList, PurchaseItemRequest request, UUID organizationId) {
        InventoryItem inventoryItem = resolveInventoryItem(request.requestedInventoryItemId(), organizationId);
        Brand brand = resolveBrand(request.brandId(), organizationId);
        String itemNameSnapshot = resolveItemName(request.itemNameSnapshot(), inventoryItem);

        PurchaseItem item = new PurchaseItem();
        item.setOrganization(purchaseList.getOrganization());
        item.setPurchaseList(purchaseList);

        purchaseMapper.updatePurchaseItem(item, request, inventoryItem, brand, itemNameSnapshot);

        return purchaseItemRepository.save(item);
    }

    private void setOptionalRelations(PurchaseList purchaseList, PurchaseListRequest request, UUID organizationId) {
        if (request.propertyId() == null) {
            purchaseList.setProperty(null);
        } else {
            var property = propertyRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Property not found"));

            purchaseList.setProperty(property);
        }

        if (request.cityId() == null) {
            purchaseList.setCity(null);
        } else {
            var city = cityRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.cityId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("City not found"));

            purchaseList.setCity(city);
        }

        if (request.supplierId() == null) {
            purchaseList.setSupplier(null);
        } else {
            var supplier = supplierRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.supplierId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Supplier not found"));

            purchaseList.setSupplier(supplier);
        }
    }

    private InventoryItem resolveInventoryItem(UUID inventoryItemId, UUID organizationId) {
        if (inventoryItemId == null) {
            return null;
        }

        return inventoryItemRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(inventoryItemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));
    }

    private Brand resolveBrand(UUID brandId, UUID organizationId) {
        if (brandId == null) {
            return null;
        }

        return brandRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(brandId, organizationId)
                .orElseThrow(() -> new NotFoundException("Brand not found"));
    }

    private String resolveItemName(String requestedName, InventoryItem inventoryItem) {
        if (requestedName != null && !requestedName.isBlank()) {
            return requestedName.trim();
        }

        if (inventoryItem != null) {
            return inventoryItem.getName();
        }

        throw new BadRequestException("Item name is required when inventory item is not provided");
    }

    private void replaceItems(PurchaseList purchaseList, List<PurchaseItemRequest> itemRequests, UUID organizationId) {
        List<PurchaseItem> currentItems = purchaseItemRepository.findByPurchaseList_IdOrderByCreatedAtAsc(
                purchaseList.getId()
        );

        purchaseItemRepository.deleteAll(currentItems);
        purchaseItemRepository.flush();

        for (PurchaseItemRequest itemRequest : itemRequests) {
            createItemEntity(purchaseList, itemRequest, organizationId);
        }
    }
}
