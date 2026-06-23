package com.tamias.organization.dto;

import com.tamias.organization.enums.OrganizationStatus;
import jakarta.validation.constraints.NotNull;

public record OrganizationStatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrganizationStatus status
) {
}
