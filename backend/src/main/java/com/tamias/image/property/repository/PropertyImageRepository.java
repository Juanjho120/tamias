package com.tamias.image.property.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.property.entity.PropertyImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, UUID> {

    List<PropertyImage> findByProperty_IdAndOrganization_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID propertyId,
            UUID organizationId
    );

    Optional<PropertyImage> findByIdAndProperty_IdAndOrganization_IdAndDeletedAtIsNull(
            UUID id,
            UUID propertyId,
            UUID organizationId
    );

    List<PropertyImage> findByProperty_IdAndOrganization_IdAndCoverAndDeletedAtIsNull(
            UUID propertyId,
            UUID organizationId,
            Boolean cover
    );

    long countByProperty_IdAndOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID propertyId,
            UUID organizationId,
            ImageStatus status
    );
}
