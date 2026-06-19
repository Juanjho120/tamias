package com.tamias.image.reservation.repository;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.reservation.entity.ReservationImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationImageRepository extends JpaRepository<ReservationImage, UUID> {

    List<ReservationImage> findByReservation_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
        UUID reservationId,
        UUID organizationId,
        ImageStatus status
    );

    Optional<ReservationImage> findByIdAndReservation_IdAndOrganization_IdAndStatus(
        UUID id,
        UUID reservationId,
        UUID organizationId,
        ImageStatus status
    );
}
