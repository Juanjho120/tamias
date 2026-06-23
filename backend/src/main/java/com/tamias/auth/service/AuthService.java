package com.tamias.auth.service;

import com.tamias.auth.dto.AuthOrganizationResponse;
import com.tamias.auth.dto.AuthUserResponse;
import com.tamias.auth.dto.LoginRequest;
import com.tamias.auth.dto.LoginResponse;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.entity.Organization;
import com.tamias.security.jwt.JwtTokenProvider;
import com.tamias.security.model.AuthenticatedUser;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
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
    private final FileStorageService fileStorageService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository,
            FileStorageService fileStorageService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email())
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Invalid credentials"));

        UserOrganization userOrganization = userOrganizationRepository
                .findFirstByUserIdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User has no active organization"));

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

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
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserOrganization userOrganization = userOrganizationRepository
                .findFirstByUserIdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User has no active organization"));

        return new LoginResponse(
                null,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toAuthUserResponse(user, userOrganization)
        );
    }

    private AuthUserResponse toAuthUserResponse(User user, UserOrganization userOrganization) {
        return new AuthUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                userOrganization.getRole().getCode().name(),
                toAuthOrganizationResponse(userOrganization.getOrganization()),
                user.isPasswordChangeRequired()
        );
    }

    private AuthOrganizationResponse toAuthOrganizationResponse(Organization organization) {
        return new AuthOrganizationResponse(
                organization.getId(),
                organization.getName(),
                buildLogoUrl(organization)
        );
    }

    private String buildLogoUrl(Organization organization) {
        String logoS3Key = organization.getLogoS3Key();
        if (logoS3Key == null || logoS3Key.isBlank()) {
            return null;
        }

        return fileStorageService.buildFileUrl(logoS3Key);
    }
}
