package com.tamias.auth.dto;

import java.util.UUID;

public record AuthUserResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String role,
    AuthOrganizationResponse organization,
    boolean passwordChangeRequired
) {
}
