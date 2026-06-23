package com.tamias.security.model;

import com.tamias.user.enums.RoleCode;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
        UUID id,
        UUID organizationId,
        String email,
        String passwordHash,
        RoleCode role
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == RoleCode.SUPER_ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"),
                    new SimpleGrantedAuthority("ROLE_PROPERTY_MANAGER"),
                    new SimpleGrantedAuthority("ROLE_MAINTENANCE_STAFF"),
                    new SimpleGrantedAuthority("ROLE_READ_ONLY")
            );
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
