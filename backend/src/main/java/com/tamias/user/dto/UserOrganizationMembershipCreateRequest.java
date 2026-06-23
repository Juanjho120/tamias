package com.tamias.user.dto;

import com.tamias.user.enums.RoleCode;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserOrganizationMembershipCreateRequest(
        @NotNull UUID organizationId,
        @NotNull RoleCode role
) {
}
