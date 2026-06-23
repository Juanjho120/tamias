package com.tamias.profile.service;

import com.tamias.auth.dto.AuthOrganizationResponse;
import com.tamias.auth.dto.AuthUserResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.entity.Organization;
import com.tamias.profile.dto.ChangePasswordRequest;
import com.tamias.profile.dto.ProfileUpdateRequest;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public ProfileService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentProfile() {
        UserOrganization userOrganization = findCurrentUserOrganization();
        return toAuthUserResponse(userOrganization);
    }

    @Transactional
    public AuthUserResponse updateProfile(ProfileUpdateRequest request) {
        UserOrganization userOrganization = findCurrentUserOrganization();
        User user = userOrganization.getUser();

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        userRepository.save(user);

        return toAuthUserResponse(userOrganization);
    }

    @Transactional
    public AuthUserResponse changePassword(ChangePasswordRequest request) {
        UserOrganization userOrganization = findCurrentUserOrganization();
        User user = userOrganization.getUser();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        return toAuthUserResponse(userOrganization);
    }

    private UserOrganization findCurrentUserOrganization() {
        User user = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .filter(foundUser -> foundUser.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return userOrganizationRepository
                .findByUserIdAndOrganizationId(user.getId(), currentUserService.getCurrentOrganizationId())
                .filter(userOrganization -> userOrganization.getStatus() == UserOrganizationStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User organization not found"));
    }

    private AuthUserResponse toAuthUserResponse(UserOrganization userOrganization) {
        User user = userOrganization.getUser();
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
