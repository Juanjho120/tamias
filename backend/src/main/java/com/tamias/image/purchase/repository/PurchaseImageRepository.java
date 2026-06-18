package com.tamias.image.purchase.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.purchase.entity.PurchaseImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseImageRepository extends JpaRepository<PurchaseImage, UUID> {

    List<PurchaseImage> findByPurchaseList_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
        UUID purchaseListId,
        UUID organizationId,
        ImageStatus status
    );

    Optional<PurchaseImage> findByIdAndPurchaseList_IdAndOrganization_IdAndStatus(
        UUID id,
        UUID purchaseListId,
        UUID organizationId,
        ImageStatus status
    );
}
