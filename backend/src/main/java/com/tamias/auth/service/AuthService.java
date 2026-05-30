package com.tamias.auth.service;

import com.tamias.auth.dto.AuthOrganizationResponse;
import com.tamias.auth.dto.AuthUserResponse;
import com.tamias.auth.dto.LoginRequest;
import com.tamias.auth.dto.LoginResponse;
import com.tamias.security.jwt.JwtTokenProvider;
import com.tamias.security.model.AuthenticatedUser;
import com.tamias.user.entity.User;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow();

        var userOrganization = userOrganizationRepository
                .findFirstByUserIdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .orElseThrow();

        var authenticatedUser = new AuthenticatedUser(
                user.getId(),
                userOrganization.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                userOrganization.getRole().getCode()
        );

        String token = jwtTokenProvider.generateToken(authenticatedUser);

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toAuthUserResponse(user, userOrganization)
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUserResponse(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();

        var userOrganization = userOrganizationRepository
                .findFirstByUserIdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .orElseThrow();

        var authenticatedUser = new AuthenticatedUser(
                user.getId(),
                userOrganization.getOrganization().getId(),
                user.getEmail(),
                user.getPasswordHash(),
                userOrganization.getRole().getCode()
        );

        return new LoginResponse(
                null,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toAuthUserResponse(user, userOrganization)
        );
    }

    private AuthUserResponse toAuthUserResponse(User user, com.tamias.user.entity.UserOrganization userOrganization) {
        return new AuthUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                userOrganization.getRole().getCode().name(),
                new AuthOrganizationResponse(
                        userOrganization.getOrganization().getId(),
                        userOrganization.getOrganization().getName()
                )
        );
    }
}
