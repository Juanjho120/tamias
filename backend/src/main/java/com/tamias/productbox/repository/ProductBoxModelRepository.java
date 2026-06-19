package com.tamias.productbox.repository;

import com.tamias.productbox.entity.ProductBoxModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        """)
    Page<ProductBoxModel> findAllAvailable(
        @Param("organizationId") UUID organizationId,
        @Param("inventoryItemId") UUID inventoryItemId,
        @Param("purchaseItemId") UUID purchaseItemId,
        Pageable pageable
    );

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
              LOWER(model.name) LIKE :searchPattern
              OR LOWER(COALESCE(model.description, '')) LIKE :searchPattern
              OR LOWER(COALESCE(inventoryItem.name, '')) LIKE :searchPattern
              OR LOWER(COALESCE(purchaseItem.itemNameSnapshot, '')) LIKE :searchPattern
          )
        """)
    Page<ProductBoxModel> search(
        @Param("organizationId") UUID organizationId,
        @Param("inventoryItemId") UUID inventoryItemId,
        @Param("purchaseItemId") UUID purchaseItemId,
        @Param("searchPattern") String searchPattern,
        Pageable pageable
    );
}
