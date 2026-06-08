package com.tamias.purchase.mapper;

import com.tamias.catalog.brand.entity.Brand;
import com.tamias.catalog.material.entity.Material;
import com.tamias.purchase.dto.PurchaseItemRequest;
import com.tamias.purchase.dto.PurchaseItemResponse;
import com.tamias.purchase.dto.PurchaseItemUpdateRequest;
import com.tamias.purchase.dto.PurchaseListRequest;
import com.tamias.purchase.dto.PurchaseListResponse;
import com.tamias.purchase.dto.PurchaseListSummaryResponse;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.purchase.entity.PurchaseList;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PurchaseMapper {

    public void updatePurchaseList(PurchaseList entity, PurchaseListRequest request) {
        entity.setPurchaseDate(request.purchaseDate());
        entity.setNotes(request.notes());
        entity.setStatus(request.status());
    }

    public void updatePurchaseItem(
            PurchaseItem entity,
            PurchaseItemRequest request,
            Material material,
            Brand brand,
            String itemNameSnapshot
    ) {
        entity.setMaterial(material);
        entity.setBrand(brand);
        entity.setItemNameSnapshot(itemNameSnapshot);
        entity.setQuantity(request.quantity() != null ? request.quantity() : BigDecimal.ONE);
        entity.setUnit(request.unit());
        entity.setEstimatedPrice(request.estimatedPrice());
        entity.setPurchased(Boolean.TRUE.equals(request.purchased()));
        entity.setNotes(request.notes());
    }

    public void updatePurchaseItem(
            PurchaseItem entity,
            PurchaseItemUpdateRequest request,
            Material material,
            Brand brand,
            String itemNameSnapshot
    ) {
        entity.setMaterial(material);
        entity.setBrand(brand);
        entity.setItemNameSnapshot(itemNameSnapshot);
        entity.setQuantity(request.quantity() != null ? request.quantity() : BigDecimal.ONE);
        entity.setUnit(request.unit());
        entity.setEstimatedPrice(request.estimatedPrice());
        entity.setPurchased(Boolean.TRUE.equals(request.purchased()));
        entity.setNotes(request.notes());
    }

    public PurchaseListSummaryResponse toSummaryResponse(
            PurchaseList entity,
            long totalItems,
            long purchasedItems,
            BigDecimal estimatedTotal
    ) {
        var property = entity.getProperty();
        var city = entity.getCity();
        var supplier = entity.getSupplier();

        return new PurchaseListSummaryResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                city != null ? city.getId() : null,
                city != null ? city.getName() : null,
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getName() : null,
                entity.getPurchaseDate(),
                entity.getStatus(),
                totalItems,
                purchasedItems,
                estimatedTotal,
                entity.getCreatedAt()
        );
    }

    public PurchaseListResponse toResponse(
            PurchaseList entity,
            List<PurchaseItem> items,
            BigDecimal estimatedTotal
    ) {
        var property = entity.getProperty();
        var city = entity.getCity();
        var supplier = entity.getSupplier();

        return new PurchaseListResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                city != null ? city.getId() : null,
                city != null ? city.getName() : null,
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getName() : null,
                entity.getPurchaseDate(),
                entity.getNotes(),
                entity.getStatus(),
                items.stream().map(this::toItemResponse).toList(),
                estimatedTotal,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PurchaseItemResponse toItemResponse(PurchaseItem entity) {
        var material = entity.getMaterial();
        var brand = entity.getBrand();

        return new PurchaseItemResponse(
                entity.getId(),
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                brand != null ? brand.getId() : null,
                brand != null ? brand.getName() : null,
                entity.getItemNameSnapshot(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getEstimatedPrice(),
                entity.getPurchased(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
