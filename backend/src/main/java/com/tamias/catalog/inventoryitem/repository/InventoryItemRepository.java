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
              AND (
                    :search IS NULL
                    OR LOWER(item.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(item.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(item.internalCode, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(item.barcode, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<InventoryItem> search(
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
