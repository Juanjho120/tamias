package com.tamias.productbox.mapper;

import com.tamias.catalog.brand.entity.Brand;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.productbox.dto.ProductBoxModelRequest;
import com.tamias.productbox.dto.ProductBoxModelResponse;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.purchase.entity.PurchaseList;
import org.springframework.stereotype.Component;

@Component
public class ProductBoxModelMapper {

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
            entity.getUpdatedAt()
        );
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
