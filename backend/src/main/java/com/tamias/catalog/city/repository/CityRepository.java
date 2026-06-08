package com.tamias.catalog.city.repository;

import com.tamias.catalog.city.entity.City;
import com.tamias.catalog.enums.CatalogStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {

    Optional<City> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String name);

    Page<City> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<City> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            CatalogStatus status,
            Pageable pageable
    );
}
