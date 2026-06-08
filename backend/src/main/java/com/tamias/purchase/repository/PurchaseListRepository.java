package com.tamias.purchase.repository;

import com.tamias.purchase.entity.PurchaseList;
import com.tamias.purchase.enums.PurchaseListStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseListRepository extends JpaRepository<PurchaseList, UUID> {

    Optional<PurchaseList> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<PurchaseList> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<PurchaseList> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            PurchaseListStatus status,
            Pageable pageable
    );

    Page<PurchaseList> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<PurchaseList> findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            PurchaseListStatus status,
            Pageable pageable
    );

    Page<PurchaseList> findByOrganization_IdAndSupplier_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID supplierId,
            Pageable pageable
    );

    Page<PurchaseList> findByOrganization_IdAndCity_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID cityId,
            Pageable pageable
    );
}
