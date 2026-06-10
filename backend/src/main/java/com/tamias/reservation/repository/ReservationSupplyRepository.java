package com.tamias.reservation.repository;

import com.tamias.reservation.entity.ReservationSupply;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSupplyRepository extends JpaRepository<ReservationSupply, UUID> {

    List<ReservationSupply> findByReservation_IdAndOrganization_IdOrderByCreatedAtAsc(
            UUID reservationId,
            UUID organizationId
    );

    Optional<ReservationSupply> findByIdAndReservation_IdAndOrganization_Id(
            UUID id,
            UUID reservationId,
            UUID organizationId
    );

    void deleteByReservation_Id(UUID reservationId);
}
