package com.tamias.auth.service;

import com.tamias.auth.dto.AuthOrganizationOptionResponse;
import com.tamias.auth.dto.AuthOrganizationResponse;
import com.tamias.auth.dto.AuthUserResponse;
import com.tamias.auth.dto.LoginRequest;
import com.tamias.auth.dto.LoginResponse;
import com.tamias.auth.dto.SwitchOrganizationRequest;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.enums.OrganizationStatus;
import com.tamias.security.jwt.JwtTokenProvider;
import com.tamias.security.model.AuthenticatedUser;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
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

        User user = findActiveUserByEmail(request.email());
        UserOrganization userOrganization = findDefaultActiveMembership(user.getId());

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user, userOrganization, true);
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUserResponse(UUID userId, UUID organizationId) {
        User user = findActiveUser(userId);
        UserOrganization userOrganization = findActiveMembership(user.getId(), organizationId);

        return buildLoginResponse(user, userOrganization, false);
    }

    @Transactional(readOnly = true)
    public List<AuthOrganizationOptionResponse> findAvailableOrganizations(UUID userId, UUID currentOrganizationId) {
        User user = findActiveUser(userId);

        return userOrganizationRepository.findByUser_IdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .stream()
                .filter(this::isUsableMembership)
                .map((userOrganization) -> toOrganizationOptionResponse(userOrganization, currentOrganizationId))
                .sorted(Comparator.comparing(
                        AuthOrganizationOptionResponse::name,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }

    @Transactional
    public LoginResponse switchOrganization(UUID userId, SwitchOrganizationRequest request) {
        User user = findActiveUser(userId);
        UserOrganization userOrganization = findActiveMembership(user.getId(), request.organizationId());

        return buildLoginResponse(user, userOrganization, true);
    }

    private LoginResponse buildLoginResponse(User user, UserOrganization userOrganization, boolean includeAccessToken) {
        String token = null;

        if (includeAccessToken) {
            var authenticatedUser = new AuthenticatedUser(
                    user.getId(),
                    userOrganization.getOrganization().getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    userOrganization.getRole().getCode()
            );
            token = jwtTokenProvider.generateToken(authenticatedUser);
        }

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toAuthUserResponse(user, userOrganization)
        );
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private User findActiveUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Invalid credentials"));
    }

    private UserOrganization findDefaultActiveMembership(UUID userId) {
        return userOrganizationRepository.findByUser_IdAndStatus(userId, UserOrganizationStatus.ACTIVE)
                .stream()
                .filter(this::isUsableMembership)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User has no active organization"));
    }

    private UserOrganization findActiveMembership(UUID userId, UUID organizationId) {
        return userOrganizationRepository.findByUser_IdAndOrganization_Id(userId, organizationId)
                .filter(userOrganization -> userOrganization.getStatus() == UserOrganizationStatus.ACTIVE)
                .filter(this::isUsableMembership)
                .orElseThrow(() -> new NotFoundException("Organization is not available for this user"));
    }

    private boolean isUsableMembership(UserOrganization userOrganization) {
        Organization organization = userOrganization.getOrganization();
        return userOrganization.getStatus() == UserOrganizationStatus.ACTIVE
                && organization != null
                && organization.getStatus() == OrganizationStatus.ACTIVE
                && organization.getDeletedAt() == null;
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

    private AuthOrganizationOptionResponse toOrganizationOptionResponse(
            UserOrganization userOrganization,
            UUID currentOrganizationId
    ) {
        Organization organization = userOrganization.getOrganization();
        return new AuthOrganizationOptionResponse(
                organization.getId(),
                organization.getName(),
                userOrganization.getRole().getCode().name(),
                buildLogoUrl(organization),
                organization.getId().equals(currentOrganizationId)
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
