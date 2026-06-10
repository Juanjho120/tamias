package com.tamias.reservation.repository;

import com.tamias.reservation.entity.Reservation;
import com.tamias.reservation.enums.ReservationStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<Reservation> findByOrganization_IdAndDeletedAtIsNull(
        UUID organizationId,
        Pageable pageable
    );

    Page<Reservation> findByOrganization_IdAndStatusAndDeletedAtIsNull(
        UUID organizationId,
        ReservationStatus status,
        Pageable pageable
    );

    Page<Reservation> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
        UUID organizationId,
        UUID propertyId,
        Pageable pageable
    );

    Page<Reservation> findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
        UUID organizationId,
        UUID propertyId,
        ReservationStatus status,
        Pageable pageable
    );

    Page<Reservation> findByOrganization_IdAndCheckInLessThanAndCheckOutGreaterThanAndDeletedAtIsNull(
        UUID organizationId,
        LocalDate endDate,
        LocalDate startDate,
        Pageable pageable
    );

    boolean existsByOrganization_IdAndProperty_IdAndStatusNotAndDeletedAtIsNullAndCheckInLessThanAndCheckOutGreaterThan(
        UUID organizationId,
        UUID propertyId,
        ReservationStatus excludedStatus,
        LocalDate checkOut,
        LocalDate checkIn
    );

    boolean existsByOrganization_IdAndProperty_IdAndIdNotAndStatusNotAndDeletedAtIsNullAndCheckInLessThanAndCheckOutGreaterThan(
        UUID organizationId,
        UUID propertyId,
        UUID excludedReservationId,
        ReservationStatus excludedStatus,
        LocalDate checkOut,
        LocalDate checkIn
    );
}
