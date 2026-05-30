package com.tamias.property.repository;

import com.tamias.property.entity.Property;
import com.tamias.property.enums.PropertyStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, UUID> {

    Optional<Property> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByOrganization_IdAndNameIgnoreCaseAndDeletedAtIsNull(UUID organizationId, String name);

    Page<Property> findByOrganization_IdAndDeletedAtIsNull(
            UUID organizationId,
            Pageable pageable
    );

    Page<Property> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            PropertyStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Property p
            WHERE p.organization.id = :organizationId
              AND p.deletedAt IS NULL
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Property> searchByText(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT p
            FROM Property p
            WHERE p.organization.id = :organizationId
              AND p.deletedAt IS NULL
              AND p.status = :status
              AND (
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.address, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<Property> searchByStatusAndText(
            @Param("organizationId") UUID organizationId,
            @Param("status") PropertyStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
