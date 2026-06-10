package com.tamias.catalog.inventoryitem.repository;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.repository.BaseCatalogRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

public interface InventoryItemRepository extends BaseCatalogRepository<InventoryItem> {

    @Query("""
            SELECT item
            FROM InventoryItem item
            WHERE item.organization.id = :organizationId
              AND item.deletedAt IS NULL
              AND (:status IS NULL OR item.status = :status)
              AND (:itemType IS NULL OR item.itemType = :itemType)
              AND (:availableForMaintenance IS NULL OR item.availableForMaintenance = :availableForMaintenance)
              AND (:availableForReservations IS NULL OR item.availableForReservations = :availableForReservations)
              AND (:availableForPurchases IS NULL OR item.availableForPurchases = :availableForPurchases)
            """)
    Page<InventoryItem> search(
            UUID organizationId,
            CatalogStatus status,
            InventoryItemType itemType,
            Boolean availableForMaintenance,
            Boolean availableForReservations,
            Boolean availableForPurchases,
            Pageable pageable
    );

    @Query("""
            SELECT item
            FROM InventoryItem item
            WHERE item.organization.id = :organizationId
              AND item.deletedAt IS NULL
              AND (:status IS NULL OR item.status = :status)
              AND (:itemType IS NULL OR item.itemType = :itemType)
              AND (:availableForMaintenance IS NULL OR item.availableForMaintenance = :availableForMaintenance)
              AND (:availableForReservations IS NULL OR item.availableForReservations = :availableForReservations)
              AND (:availableForPurchases IS NULL OR item.availableForPurchases = :availableForPurchases)
              AND (
                    LOWER(item.name) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(item.description, '')) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(item.internalCode, '')) LIKE CONCAT('%', LOWER(:search), '%')
                    OR LOWER(COALESCE(item.barcode, '')) LIKE CONCAT('%', LOWER(:search), '%')
              )
            """)
    Page<InventoryItem> searchWithText(
            UUID organizationId,
            CatalogStatus status,
            InventoryItemType itemType,
            Boolean availableForMaintenance,
            Boolean availableForReservations,
            Boolean availableForPurchases,
            String search,
            Pageable pageable
    );
}
