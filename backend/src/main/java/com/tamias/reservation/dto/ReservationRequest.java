package com.tamias.reservation.dto;

import com.tamias.reservation.enums.ReservationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "Property is required")
        UUID propertyId,

        UUID platformId,

        @Size(max = 150, message = "Reservation code must not exceed 150 characters")
        String reservationCode,

        @NotNull(message = "Check-in date is required")
        LocalDate checkIn,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOut,

        Boolean suppliesDelivered,

        String observations,

        @DecimalMin(value = "0.00", message = "Reservation value must be greater than or equal to 0")
        BigDecimal reservationValue,

        @Size(max = 100, message = "Invoice number must not exceed 100 characters")
        String invoiceNumber,

        @Size(max = 100, message = "Invoice series must not exceed 100 characters")
        String invoiceSeries,

        @NotNull(message = "Status is required")
        ReservationStatus status,

        @Valid
        List<ReservationGuestRequest> guests
) {
}
