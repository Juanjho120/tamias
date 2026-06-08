package com.tamias.reservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReservationGuestRequest(
        UUID guestId,

        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        Boolean primary
) {
}
