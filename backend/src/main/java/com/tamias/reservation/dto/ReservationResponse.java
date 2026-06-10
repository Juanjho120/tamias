package com.tamias.reservation.dto;

import com.tamias.reservation.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID platformId,
        String platformName,
        String reservationCode,
        LocalDate checkIn,
        LocalDate checkOut,
        Boolean suppliesDelivered,
        String observations,
        BigDecimal reservationValue,
        String invoiceNumber,
        String invoiceSeries,
        List<ReservationGuestResponse> guests,
        List<ReservationSupplyResponse> supplies,
        ReservationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
