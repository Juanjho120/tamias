package com.tamias.reservation.repository;

import com.tamias.reservation.entity.ReservationGuest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationGuestRepository extends JpaRepository<ReservationGuest, UUID> {

    List<ReservationGuest> findByReservation_Id(UUID reservationId);

    void deleteByReservation_Id(UUID reservationId);
}
