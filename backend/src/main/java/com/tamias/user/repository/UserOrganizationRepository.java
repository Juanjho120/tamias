package com.tamias.user.repository;

import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {

    Optional<UserOrganization> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    Optional<UserOrganization> findFirstByUserIdAndStatus(UUID userId, UserOrganizationStatus status);
}
