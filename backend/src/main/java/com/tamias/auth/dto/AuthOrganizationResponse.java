package com.tamias.auth.dto;

import java.util.UUID;

public record AuthOrganizationResponse(
        UUID id,
        String name
) {
}
