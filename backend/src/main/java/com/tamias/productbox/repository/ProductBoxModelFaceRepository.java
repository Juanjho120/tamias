package com.tamias.productbox.repository;

import com.tamias.productbox.entity.ProductBoxModelFace;
import com.tamias.productbox.enums.ProductBoxFaceName;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBoxModelFaceRepository extends JpaRepository<ProductBoxModelFace, UUID> {

    List<ProductBoxModelFace> findByProductBoxModel_IdAndOrganization_IdOrderByFaceNameAsc(
        UUID productBoxModelId,
        UUID organizationId
    );

    Optional<ProductBoxModelFace> findByProductBoxModel_IdAndOrganization_IdAndFaceName(
        UUID productBoxModelId,
        UUID organizationId,
        ProductBoxFaceName faceName
    );
}
