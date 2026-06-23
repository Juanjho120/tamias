package com.tamias.user.repository;

import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {

    Optional<UserOrganization> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Optional<UserOrganization> findFirstByUserIdAndStatus(UUID userId, UserOrganizationStatus status);

    Optional<UserOrganization> findByUser_IdAndOrganization_Id(UUID userId, UUID organizationId);

    List<UserOrganization> findByUser_IdAndStatus(UUID userId, UserOrganizationStatus status);

    Page<UserOrganization> findByOrganization_IdAndStatus(
            UUID organizationId,
            UserOrganizationStatus status,
            Pageable pageable
    );
}
