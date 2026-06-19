package com.tamias.productbox.repository;

import com.tamias.productbox.entity.ProductBoxModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductBoxModelRepository extends JpaRepository<ProductBoxModel, UUID> {

    Optional<ProductBoxModel> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    @Query("""
        SELECT model
        FROM ProductBoxModel model
        LEFT JOIN model.inventoryItem inventoryItem
        LEFT JOIN model.purchaseItem purchaseItem
        WHERE model.organization.id = :organizationId
          AND model.deletedAt IS NULL
          AND (:inventoryItemId IS NULL OR inventoryItem.id = :inventoryItemId)
          AND (:purchaseItemId IS NULL OR purchaseItem.id = :purchaseItemId)
          AND (
              :search IS NULL
              OR LOWER(model.name) LIKE CONCAT('%', LOWER(:search), '%')
              OR LOWER(COALESCE(model.description, '')) LIKE CONCAT('%', LOWER(:search), '%')
              OR LOWER(COALESCE(inventoryItem.name, '')) LIKE CONCAT('%', LOWER(:search), '%')
              OR LOWER(COALESCE(purchaseItem.itemNameSnapshot, '')) LIKE CONCAT('%', LOWER(:search), '%')
          )
        """)
    Page<ProductBoxModel> search(
        UUID organizationId,
        UUID inventoryItemId,
        UUID purchaseItemId,
        String search,
        Pageable pageable
    );
}
