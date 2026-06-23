package com.tamias.auth.dto;

import java.util.UUID;

public record AuthOrganizationOptionResponse(
        UUID id,
        String name,
        String role,
        String logoUrl,
        boolean current
) {
}
