package com.tamias.reservation.dto;

import java.util.UUID;

public record ReservationGuestResponse(
        UUID id,
        UUID guestId,
        String fullName,
        String phone,
        Boolean primary
) {
}
