package com.tamias.guest.repository;

import com.tamias.guest.entity.Guest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, UUID> {

    Optional<Guest> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);
}
