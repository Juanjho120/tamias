package com.tamias.security.service;

import com.tamias.security.model.AuthenticatedUser;
import com.tamias.user.entity.User;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository
    ) {
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        var userOrganization = userOrganizationRepository
                .findFirstByUserIdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .orElseThrow(() -> new UsernameNotFoundException("User has no active organization"));

        return new AuthenticatedUser(
                user.getId(),
                userOrganization.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                userOrganization.getRole().getCode()
        );
    }
}
