package com.tamias.image.inventoryitem.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.inventoryitem.entity.InventoryItemImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemImageRepository extends JpaRepository<InventoryItemImage, UUID> {

  List<InventoryItemImage> findByInventoryItem_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
      UUID inventoryItemId,
      UUID organizationId,
      ImageStatus status
  );

  Optional<InventoryItemImage> findByIdAndInventoryItem_IdAndOrganization_IdAndStatus(
      UUID id,
      UUID inventoryItemId,
      UUID organizationId,
      ImageStatus status
  );

  List<InventoryItemImage> findByInventoryItem_IdAndOrganization_IdAndCoverAndStatus(
      UUID inventoryItemId,
      UUID organizationId,
      Boolean cover,
      ImageStatus status
  );

  long countByInventoryItem_IdAndOrganization_IdAndStatus(
      UUID inventoryItemId,
      UUID organizationId,
      ImageStatus status
  );
}
