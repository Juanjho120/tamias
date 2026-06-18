package com.tamias.image.property.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.property.entity.PropertyImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    List<PropertyImage> findByProperty_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
            UUID propertyId,
            UUID organizationId,
            ImageStatus status
    );

    Optional<PropertyImage> findByIdAndProperty_IdAndOrganization_IdAndStatus(
            UUID id,
            UUID propertyId,
            UUID organizationId,
            ImageStatus status
    );

    List<PropertyImage> findByProperty_IdAndOrganization_IdAndCoverAndStatus(
            UUID propertyId,
            UUID organizationId,
            Boolean cover,
            ImageStatus status
    );

    long countByProperty_IdAndOrganization_IdAndStatus(
            UUID propertyId,
            UUID organizationId,
            ImageStatus status
    );
}
