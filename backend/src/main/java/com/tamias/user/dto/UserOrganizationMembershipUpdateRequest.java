package com.tamias.user.dto;

import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserOrganizationStatus;

import jakarta.validation.constraints.NotNull;

public record UserOrganizationMembershipUpdateRequest(
        @NotNull RoleCode role,
        @NotNull UserOrganizationStatus status
) {
}
