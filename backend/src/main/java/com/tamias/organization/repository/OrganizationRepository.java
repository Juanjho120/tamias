package com.tamias.organization.repository;

import com.tamias.organization.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);

    Page<Organization> findByDeletedAtIsNull(Pageable pageable);

    boolean existsByNameIgnoreCase(String name);
}
