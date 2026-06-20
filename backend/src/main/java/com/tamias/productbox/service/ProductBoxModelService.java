package com.tamias.productbox.service;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.productbox.dto.ProductBoxModelRequest;
import com.tamias.productbox.dto.ProductBoxModelResponse;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.entity.ProductBoxModelFace;
import com.tamias.productbox.mapper.ProductBoxModelMapper;
import com.tamias.productbox.repository.ProductBoxModelFaceRepository;
import com.tamias.productbox.repository.ProductBoxModelRepository;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.purchase.enums.PurchaseListStatus;
import com.tamias.purchase.repository.PurchaseItemRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductBoxModelService {

    private final ProductBoxModelRepository productBoxModelRepository;
    private final ProductBoxModelFaceRepository productBoxModelFaceRepository;
    private final OrganizationRepository organizationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ProductBoxModelMapper productBoxModelMapper;

    public ProductBoxModelService(
        ProductBoxModelRepository productBoxModelRepository,
        ProductBoxModelFaceRepository productBoxModelFaceRepository,
        OrganizationRepository organizationRepository,
        InventoryItemRepository inventoryItemRepository,
        PurchaseItemRepository purchaseItemRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        ProductBoxModelMapper productBoxModelMapper
    ) {
        this.productBoxModelRepository = productBoxModelRepository;
        this.productBoxModelFaceRepository = productBoxModelFaceRepository;
        this.organizationRepository = organizationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.productBoxModelMapper = productBoxModelMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<ProductBoxModelResponse> findAll(
        UUID inventoryItemId,
        UUID purchaseItemId,
        String search,
        Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String searchPattern = buildSearchPattern(search);
        Page<ProductBoxModel> page = searchPattern == null
            ? productBoxModelRepository.findAllAvailable(organizationId, inventoryItemId, purchaseItemId, pageable)
            : productBoxModelRepository.search(organizationId, inventoryItemId, purchaseItemId, searchPattern, pageable);

        return PageResponse.from(page.map(productBoxModelMapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ProductBoxModelResponse findById(UUID id) {
        return productBoxModelMapper.toResponse(findEntity(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelResponse create(ProductBoxModelRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = findCurrentUser();

        ProductBoxModel entity = new ProductBoxModel();
        entity.setOrganization(organization);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        productBoxModelMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);

        return productBoxModelMapper.toResponse(productBoxModelRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelResponse update(UUID id, ProductBoxModelRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        ProductBoxModel entity = findEntity(id);
        User currentUser = findCurrentUser();

        productBoxModelMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);
        entity.setUpdatedBy(currentUser);

        return productBoxModelMapper.toResponse(productBoxModelRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        ProductBoxModel entity = findEntity(id);
        User currentUser = findCurrentUser();
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        List<ProductBoxModelFace> faces = productBoxModelFaceRepository
            .findByProductBoxModel_IdAndOrganization_IdOrderByFaceNameAsc(id, organizationId);

        for (ProductBoxModelFace face : faces) {
            deleteAllStorageKeys(face);
        }

        productBoxModelFaceRepository.deleteAll(faces);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        productBoxModelRepository.save(entity);
    }

    private ProductBoxModel findEntity(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return productBoxModelRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Product box model not found"));
    }

    private void setOptionalRelations(ProductBoxModel entity, ProductBoxModelRequest request, UUID organizationId) {
        InventoryItem inventoryItem = resolveInventoryItem(request.inventoryItemId(), organizationId);
        PurchaseItem purchaseItem = resolvePurchaseItem(request.purchaseItemId(), organizationId);

        if (
            inventoryItem != null
                && purchaseItem != null
                && purchaseItem.getInventoryItem() != null
                && !purchaseItem.getInventoryItem().getId().equals(inventoryItem.getId())
        ) {
            throw new BadRequestException("Purchase item is linked to a different inventory item");
        }

        entity.setInventoryItem(inventoryItem);
        entity.setPurchaseItem(purchaseItem);
    }

    private InventoryItem resolveInventoryItem(UUID inventoryItemId, UUID organizationId) {
        if (inventoryItemId == null) {
            return null;
        }

        InventoryItem inventoryItem = inventoryItemRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(inventoryItemId, organizationId)
            .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        if (inventoryItem.getStatus() == CatalogStatus.DELETED) {
            throw new BadRequestException("Inventory item is not available");
        }

        return inventoryItem;
    }

    private PurchaseItem resolvePurchaseItem(UUID purchaseItemId, UUID organizationId) {
        if (purchaseItemId == null) {
            return null;
        }

        PurchaseItem purchaseItem = purchaseItemRepository
            .findAvailableByIdAndOrganizationId(purchaseItemId, organizationId)
            .orElseThrow(() -> new NotFoundException("Purchase item not found"));

        if (purchaseItem.getPurchaseList().getStatus() == PurchaseListStatus.DELETED) {
            throw new BadRequestException("Purchase item is not available");
        }

        return purchaseItem;
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String buildSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private void deleteAllStorageKeys(ProductBoxModelFace face) {
        List<String> keysToDelete = new ArrayList<>();
        addIfPresent(keysToDelete, face.getS3Key());
        addIfPresent(keysToDelete, face.getOriginalS3Key());
        addIfPresent(keysToDelete, face.getProcessedS3Key());

        for (String storageKey : keysToDelete) {
            fileStorageService.delete(storageKey);
        }
    }

    private void addIfPresent(List<String> storageKeys, String storageKey) {
        if (storageKey != null && !storageKey.isBlank() && !storageKeys.contains(storageKey)) {
            storageKeys.add(storageKey);
        }
    }
}
