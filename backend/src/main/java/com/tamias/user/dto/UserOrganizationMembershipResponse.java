package com.tamias.user.dto;

import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserOrganizationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserOrganizationMembershipResponse(
        UUID organizationId,
        String organizationName,
        String organizationLogoUrl,
        RoleCode role,
        UserOrganizationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
