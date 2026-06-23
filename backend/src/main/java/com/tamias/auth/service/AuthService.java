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
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.jwt.JwtTokenProvider;
import com.tamias.security.model.AuthenticatedUser;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
    private final OrganizationRepository organizationRepository;
    private final FileStorageService fileStorageService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository,
            OrganizationRepository organizationRepository,
            FileStorageService fileStorageService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.organizationRepository = organizationRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = findActiveUserByEmail(request.email());
        user.setLastLoginAt(OffsetDateTime.now());

        if (isGlobalSuperAdmin(user)) {
            Organization organization = findLoginOrganizationForGlobalSuperAdmin(user);
            rememberLastOrganization(user, organization.getId());
            userRepository.save(user);
            return buildLoginResponse(user, organization, RoleCode.SUPER_ADMIN, true);
        }

        UserOrganization userOrganization = findLoginMembership(user);
        rememberLastOrganization(user, userOrganization.getOrganization().getId());
        userRepository.save(user);
        return buildLoginResponse(user, userOrganization, true);
    }

    @Transactional(readOnly = true)
    public LoginResponse getCurrentUserResponse(UUID userId, UUID organizationId) {
        User user = findActiveUser(userId);

        if (isGlobalSuperAdmin(user)) {
            Organization organization = findActiveOrganization(organizationId);
            return buildLoginResponse(user, organization, RoleCode.SUPER_ADMIN, false);
        }

        UserOrganization userOrganization = findActiveMembership(user.getId(), organizationId);
        return buildLoginResponse(user, userOrganization, false);
    }

    @Transactional(readOnly = true)
    public List<AuthOrganizationOptionResponse> findAvailableOrganizations(UUID userId, UUID currentOrganizationId) {
        User user = findActiveUser(userId);

        if (isGlobalSuperAdmin(user)) {
            return organizationRepository.findByStatusAndDeletedAtIsNull(OrganizationStatus.ACTIVE)
                    .stream()
                    .map((organization) -> toOrganizationOptionResponse(
                            organization,
                            RoleCode.SUPER_ADMIN,
                            currentOrganizationId
                    ))
                    .sorted(Comparator.comparing(
                            AuthOrganizationOptionResponse::name,
                            String.CASE_INSENSITIVE_ORDER
                    ))
                    .toList();
        }

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

        if (isGlobalSuperAdmin(user)) {
            Organization organization = findActiveOrganization(request.organizationId());
            rememberLastOrganization(user, organization.getId());
            userRepository.save(user);
            return buildLoginResponse(user, organization, RoleCode.SUPER_ADMIN, true);
        }

        UserOrganization userOrganization = findActiveMembership(user.getId(), request.organizationId());
        rememberLastOrganization(user, userOrganization.getOrganization().getId());
        userRepository.save(user);
        return buildLoginResponse(user, userOrganization, true);
    }

    private LoginResponse buildLoginResponse(User user, UserOrganization userOrganization, boolean includeAccessToken) {
        return buildLoginResponse(
                user,
                userOrganization.getOrganization(),
                userOrganization.getRole().getCode(),
                includeAccessToken
        );
    }

    private LoginResponse buildLoginResponse(
            User user,
            Organization organization,
            RoleCode roleCode,
            boolean includeAccessToken
    ) {
        String token = null;
        if (includeAccessToken) {
            var authenticatedUser = new AuthenticatedUser(
                    user.getId(),
                    organization.getId(),
                    user.getEmail(),
                    user.getPasswordHash(),
                    roleCode
            );
            token = jwtTokenProvider.generateToken(authenticatedUser);
        }

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                toAuthUserResponse(user, organization, roleCode)
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

    private Organization findActiveOrganization(UUID organizationId) {
        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Organization is not available for this user"));
    }

    private Organization findLoginOrganizationForGlobalSuperAdmin(User user) {
        return findLastActiveOrganization(user.getLastOrganizationId())
                .orElseGet(() -> findDefaultActiveMembership(user.getId()).getOrganization());
    }

    private UserOrganization findLoginMembership(User user) {
        return findLastActiveMembership(user.getId(), user.getLastOrganizationId())
                .orElseGet(() -> findDefaultActiveMembership(user.getId()));
    }

    private Optional<Organization> findLastActiveOrganization(UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }

        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .filter(organization -> organization.getStatus() == OrganizationStatus.ACTIVE);
    }

    private Optional<UserOrganization> findLastActiveMembership(UUID userId, UUID organizationId) {
        if (organizationId == null) {
            return Optional.empty();
        }

        return userOrganizationRepository.findByUser_IdAndOrganization_Id(userId, organizationId)
                .filter(userOrganization -> userOrganization.getStatus() == UserOrganizationStatus.ACTIVE)
                .filter(this::isUsableMembership);
    }

    private UserOrganization findDefaultActiveMembership(UUID userId) {
        return userOrganizationRepository.findByUser_IdAndStatus(userId, UserOrganizationStatus.ACTIVE)
                .stream()
                .filter(this::isUsableMembership)
                .min(this::compareDefaultMembershipPriority)
                .orElseThrow(() -> new NotFoundException("User has no active organization"));
    }

    private int compareDefaultMembershipPriority(UserOrganization left, UserOrganization right) {
        boolean leftSuperAdmin = hasRole(left, RoleCode.SUPER_ADMIN);
        boolean rightSuperAdmin = hasRole(right, RoleCode.SUPER_ADMIN);

        if (leftSuperAdmin != rightSuperAdmin) {
            return leftSuperAdmin ? -1 : 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(
                left.getOrganization().getName(),
                right.getOrganization().getName()
        );
    }

    private UserOrganization findActiveMembership(UUID userId, UUID organizationId) {
        return userOrganizationRepository.findByUser_IdAndOrganization_Id(userId, organizationId)
                .filter(userOrganization -> userOrganization.getStatus() == UserOrganizationStatus.ACTIVE)
                .filter(this::isUsableMembership)
                .orElseThrow(() -> new NotFoundException("Organization is not available for this user"));
    }

    private boolean isGlobalSuperAdmin(User user) {
        return userOrganizationRepository.findByUser_IdAndStatus(user.getId(), UserOrganizationStatus.ACTIVE)
                .stream()
                .filter(this::isUsableMembership)
                .anyMatch(userOrganization -> hasRole(userOrganization, RoleCode.SUPER_ADMIN));
    }

    private boolean hasRole(UserOrganization userOrganization, RoleCode roleCode) {
        return userOrganization.getRole() != null && userOrganization.getRole().getCode() == roleCode;
    }

    private boolean isUsableMembership(UserOrganization userOrganization) {
        Organization organization = userOrganization.getOrganization();
        return userOrganization.getStatus() == UserOrganizationStatus.ACTIVE
                && organization != null
                && organization.getStatus() == OrganizationStatus.ACTIVE
                && organization.getDeletedAt() == null;
    }

    private void rememberLastOrganization(User user, UUID organizationId) {
        user.setLastOrganizationId(organizationId);
    }

    private AuthUserResponse toAuthUserResponse(User user, Organization organization, RoleCode roleCode) {
        return new AuthUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleCode.name(),
                toAuthOrganizationResponse(organization),
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
        return toOrganizationOptionResponse(
                userOrganization.getOrganization(),
                userOrganization.getRole().getCode(),
                currentOrganizationId
        );
    }

    private AuthOrganizationOptionResponse toOrganizationOptionResponse(
            Organization organization,
            RoleCode roleCode,
            UUID currentOrganizationId
    ) {
        return new AuthOrganizationOptionResponse(
                organization.getId(),
                organization.getName(),
                roleCode.name(),
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
