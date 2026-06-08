package com.tamias.catalog.supplier.repository;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.supplier.entity.Supplier;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String name);

    Page<Supplier> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<Supplier> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
