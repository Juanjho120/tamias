package com.tamias.reservation.dto;

import com.tamias.reservation.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReservationSummaryResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID platformId,
        String platformName,
        String reservationCode,
        LocalDate checkIn,
        LocalDate checkOut,
        List<String> guestNames,
        BigDecimal reservationValue,
        ReservationStatus status,
        OffsetDateTime createdAt
) {
}
