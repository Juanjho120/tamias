package com.tamias.user.dto;

import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        RoleCode role,
        UserStatus status,
        OffsetDateTime createdAt
) {
}
