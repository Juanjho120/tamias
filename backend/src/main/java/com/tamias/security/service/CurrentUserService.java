package com.tamias.security.service;

import com.tamias.security.model.AuthenticatedUser;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user found");
        }

        return user;
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().id();
    }

    public UUID getCurrentOrganizationId() {
        return getCurrentUser().organizationId();
    }

    public String getCurrentEmail() {
        return getCurrentUser().email();
    }

    public String getCurrentRole() {
        return getCurrentUser().role().name();
    }
}
