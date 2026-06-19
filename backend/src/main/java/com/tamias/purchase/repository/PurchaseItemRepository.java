package com.tamias.purchase.repository;

import com.tamias.purchase.entity.PurchaseItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    List<PurchaseItem> findByPurchaseList_IdOrderByCreatedAtAsc(UUID purchaseListId);

    Optional<PurchaseItem> findByIdAndPurchaseList_IdAndOrganization_Id(
        UUID id,
        UUID purchaseListId,
        UUID organizationId
    );

    @Query("""
        SELECT item
        FROM PurchaseItem item
        JOIN item.purchaseList purchaseList
        WHERE item.id = :id
          AND item.organization.id = :organizationId
          AND purchaseList.deletedAt IS NULL
        """)
    Optional<PurchaseItem> findAvailableByIdAndOrganizationId(UUID id, UUID organizationId);

    long countByPurchaseList_Id(UUID purchaseListId);

    long countByPurchaseList_IdAndPurchased(UUID purchaseListId, Boolean purchased);

    void deleteByPurchaseList_Id(UUID purchaseListId);

    @Query("""
        SELECT COALESCE(SUM(i.estimatedPrice * i.quantity), 0)
        FROM PurchaseItem i
        WHERE i.purchaseList.id = :purchaseListId
        """)
    BigDecimal calculateEstimatedTotal(UUID purchaseListId);
}
