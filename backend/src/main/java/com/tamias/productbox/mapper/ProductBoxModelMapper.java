package com.tamias.productbox.mapper;

import com.tamias.catalog.brand.entity.Brand;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.document.storage.FileStorageService;
import com.tamias.productbox.dto.ProductBoxModelFaceResponse;
import com.tamias.productbox.dto.ProductBoxModelRequest;
import com.tamias.productbox.dto.ProductBoxModelResponse;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.entity.ProductBoxModelFace;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.purchase.entity.PurchaseList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ProductBoxModelMapper {

    private final FileStorageService fileStorageService;

    public ProductBoxModelMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public void updateEntity(ProductBoxModel entity, ProductBoxModelRequest request) {
        entity.setName(request.name().trim());
        entity.setDescription(normalizeNullable(request.description()));
        entity.setWidth(request.width());
        entity.setHeight(request.height());
        entity.setDepth(request.depth());
        entity.setUnit(request.unit());
    }

    public ProductBoxModelResponse toResponse(ProductBoxModel entity) {
        InventoryItem inventoryItem = entity.getInventoryItem();
        Brand brand = inventoryItem != null ? inventoryItem.getBrand() : null;
        PurchaseItem purchaseItem = entity.getPurchaseItem();
        PurchaseList purchaseList = purchaseItem != null ? purchaseItem.getPurchaseList() : null;

        return new ProductBoxModelResponse(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            inventoryItem != null ? inventoryItem.getId() : null,
            inventoryItem != null ? inventoryItem.getName() : null,
            brand != null ? brand.getId() : null,
            brand != null ? brand.getName() : null,
            purchaseItem != null ? purchaseItem.getId() : null,
            purchaseItem != null ? purchaseItem.getItemNameSnapshot() : null,
            purchaseList != null ? purchaseList.getId() : null,
            entity.getWidth(),
            entity.getHeight(),
            entity.getDepth(),
            entity.getUnit(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            toFaceMap(entity)
        );
    }

    public ProductBoxModelFaceResponse toFaceResponse(ProductBoxModelFace entity) {
        return new ProductBoxModelFaceResponse(
            entity.getId(),
            entity.getFaceName().getValue(),
            entity.getS3Key(),
            entity.getFilepath(),
            entity.getOriginalFilename(),
            entity.getContentType(),
            entity.getSizeBytes(),
            entity.getRotationDegrees(),
            entity.getFlipHorizontal(),
            entity.getFlipVertical(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            buildUrl(entity.getS3Key()),
            fileStorageService.getDownloadUrlExpirationSeconds(),
            entity.getOriginalS3Key(),
            entity.getOriginalFilepath(),
            entity.getOriginalUploadFilename(),
            entity.getOriginalContentType(),
            entity.getOriginalSizeBytes(),
            entity.getOriginalWidthPx(),
            entity.getOriginalHeightPx(),
            buildUrl(entity.getOriginalS3Key()),
            entity.getProcessedS3Key(),
            entity.getProcessedFilepath(),
            entity.getProcessedFilename(),
            entity.getProcessedContentType(),
            entity.getProcessedSizeBytes(),
            entity.getProcessedWidthPx(),
            entity.getProcessedHeightPx(),
            buildUrl(entity.getProcessedS3Key()),
            entity.getTargetAspectRatio(),
            entity.getPointsJson(),
            entity.getTextureStatus(),
            entity.getProcessingError(),
            entity.getProcessedAt(),
            entity.getAcceptedAt(),
            entity.getAutoDetectedPoints(),
            entity.getContourConfidence(),
            entity.getEnhancementMode()
        );
    }

    private Map<String, ProductBoxModelFaceResponse> toFaceMap(ProductBoxModel entity) {
        if (entity.getFaces() == null || entity.getFaces().isEmpty()) {
            return Map.of();
        }

        return entity.getFaces()
            .stream()
            .sorted(Comparator.comparingInt(face -> faceOrder(face.getFaceName())))
            .collect(Collectors.toMap(
                face -> face.getFaceName().getValue(),
                this::toFaceResponse,
                (first, second) -> first,
                LinkedHashMap::new
            ));
    }

    private int faceOrder(ProductBoxFaceName faceName) {
        return switch (faceName) {
            case FRONT -> 0;
            case BACK -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
            case TOP -> 4;
            case BOTTOM -> 5;
        };
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildUrl(String storageKey) {
        return storageKey == null || storageKey.isBlank() ? null : fileStorageService.buildFileUrl(storageKey);
    }
}
