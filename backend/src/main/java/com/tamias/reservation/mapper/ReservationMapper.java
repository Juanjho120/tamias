package com.tamias.reservation.mapper;

import com.tamias.reservation.dto.ReservationGuestResponse;
import com.tamias.reservation.dto.ReservationRequest;
import com.tamias.reservation.dto.ReservationResponse;
import com.tamias.reservation.dto.ReservationSummaryResponse;
import com.tamias.reservation.entity.Reservation;
import com.tamias.reservation.entity.ReservationGuest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public void updateEntity(Reservation entity, ReservationRequest request) {
        entity.setReservationCode(request.reservationCode());
        entity.setCheckIn(request.checkIn());
        entity.setCheckOut(request.checkOut());
        entity.setSuppliesDelivered(request.suppliesDelivered() != null ? request.suppliesDelivered() : false);
        entity.setObservations(request.observations());
        entity.setReservationValue(request.reservationValue());
        entity.setInvoiceNumber(request.invoiceNumber());
        entity.setInvoiceSeries(request.invoiceSeries());
        entity.setStatus(request.status());
    }

    public ReservationSummaryResponse toSummaryResponse(
            Reservation entity,
            List<ReservationGuest> reservationGuests
    ) {
        var property = entity.getProperty();
        var platform = entity.getPlatform();

        return new ReservationSummaryResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                platform != null ? platform.getId() : null,
                platform != null ? platform.getName() : null,
                entity.getReservationCode(),
                entity.getCheckIn(),
                entity.getCheckOut(),
                reservationGuests.stream()
                        .map(reservationGuest -> reservationGuest.getGuest().getFullName())
                        .toList(),
                entity.getReservationValue(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public ReservationResponse toResponse(
            Reservation entity,
            List<ReservationGuest> reservationGuests
    ) {
        var property = entity.getProperty();
        var platform = entity.getPlatform();

        return new ReservationResponse(
                entity.getId(),
                property.getId(),
                property.getName(),
                platform != null ? platform.getId() : null,
                platform != null ? platform.getName() : null,
                entity.getReservationCode(),
                entity.getCheckIn(),
                entity.getCheckOut(),
                entity.getSuppliesDelivered(),
                entity.getObservations(),
                entity.getReservationValue(),
                entity.getInvoiceNumber(),
                entity.getInvoiceSeries(),
                reservationGuests.stream()
                        .map(this::toGuestResponse)
                        .toList(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ReservationGuestResponse toGuestResponse(ReservationGuest reservationGuest) {
        var guest = reservationGuest.getGuest();

        return new ReservationGuestResponse(
                reservationGuest.getId(),
                guest.getId(),
                guest.getFullName(),
                guest.getPhone(),
                reservationGuest.getPrimaryGuest()
        );
    }
}
