package com.tamias.user.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.enums.OrganizationStatus;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.dto.UserOrganizationMembershipCreateRequest;
import com.tamias.user.dto.UserOrganizationMembershipResponse;
import com.tamias.user.dto.UserOrganizationMembershipUpdateRequest;
import com.tamias.user.entity.Role;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.RoleCode;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.repository.RoleRepository;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserOrganizationMembershipService {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleRepository roleRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;

    public UserOrganizationMembershipService(
            UserRepository userRepository,
            UserOrganizationRepository userOrganizationRepository,
            OrganizationRepository organizationRepository,
            RoleRepository roleRepository,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService
    ) {
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.organizationRepository = organizationRepository;
        this.roleRepository = roleRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<UserOrganizationMembershipResponse> findAll(UUID userId) {
        findActiveUser(userId);

        return userOrganizationRepository.findByUser_Id(userId)
                .stream()
                .filter((membership) -> membership.getStatus() != UserOrganizationStatus.DELETED)
                .filter((membership) -> membership.getOrganization() != null)
                .filter((membership) -> membership.getOrganization().getDeletedAt() == null)
                .sorted(Comparator.comparing(
                        (UserOrganization membership) -> membership.getOrganization().getName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserOrganizationMembershipResponse create(UUID userId, UserOrganizationMembershipCreateRequest request) {
        User user = findActiveUser(userId);
        Organization organization = findActiveOrganization(request.organizationId());
        Role role = findRole(request.role());

        UserOrganization membership = userOrganizationRepository
                .findByUser_IdAndOrganization_Id(user.getId(), organization.getId())
                .orElseGet(UserOrganization::new);

        validateLastSuperAdminAccessBeforeChange(membership, request.role(), UserOrganizationStatus.ACTIVE);

        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(role);
        membership.setStatus(UserOrganizationStatus.ACTIVE);

        return toResponse(userOrganizationRepository.save(membership));
    }

    @Transactional
    public UserOrganizationMembershipResponse update(
            UUID userId,
            UUID organizationId,
            UserOrganizationMembershipUpdateRequest request
    ) {
        findActiveUser(userId);

        if (request.status() == UserOrganizationStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete a membership");
        }

        UserOrganization membership = findExistingMembership(userId, organizationId);
        findActiveOrganization(organizationId);
        Role role = findRole(request.role());

        validateLastSuperAdminAccessBeforeChange(membership, request.role(), request.status());

        membership.setRole(role);
        membership.setStatus(request.status());

        return toResponse(userOrganizationRepository.save(membership));
    }

    @Transactional
    public void delete(UUID userId, UUID organizationId) {
        findActiveUser(userId);
        UserOrganization membership = findExistingMembership(userId, organizationId);

        validateLastSuperAdminAccessBeforeChange(
                membership,
                membership.getRole().getCode(),
                UserOrganizationStatus.DELETED
        );

        membership.setStatus(UserOrganizationStatus.DELETED);
        userOrganizationRepository.save(membership);
    }

    private User findActiveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .filter((user) -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Organization findActiveOrganization(UUID organizationId) {
        return organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .filter((organization) -> organization.getStatus() == OrganizationStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private Role findRole(RoleCode roleCode) {
        return roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("Invalid role"));
    }

    private UserOrganization findExistingMembership(UUID userId, UUID organizationId) {
        return userOrganizationRepository.findByUser_IdAndOrganization_Id(userId, organizationId)
                .filter((membership) -> membership.getStatus() != UserOrganizationStatus.DELETED)
                .orElseThrow(() -> new NotFoundException("User organization membership not found"));
    }

    private void validateLastSuperAdminAccessBeforeChange(
            UserOrganization membership,
            RoleCode nextRole,
            UserOrganizationStatus nextStatus
    ) {
        if (!isActiveSuperAdminMembership(membership)) {
            return;
        }

        boolean remainsActiveSuperAdmin = nextRole == RoleCode.SUPER_ADMIN
                && nextStatus == UserOrganizationStatus.ACTIVE;

        if (remainsActiveSuperAdmin) {
            return;
        }

        UUID userId = membership.getUser().getId();
        UUID organizationId = membership.getOrganization().getId();

        boolean hasAnotherActiveSuperAdminMembership = userOrganizationRepository.findByUser_Id(userId)
                .stream()
                .filter((candidate) -> !candidate.getOrganization().getId().equals(organizationId))
                .anyMatch(this::isActiveSuperAdminMembership);

        if (!hasAnotherActiveSuperAdminMembership) {
            if (currentUserService.getCurrentUserId().equals(userId)) {
                throw new BadRequestException("You cannot remove your last SUPER_ADMIN membership");
            }

            throw new BadRequestException("A user must keep at least one active SUPER_ADMIN membership before this change");
        }
    }

    private boolean isActiveSuperAdminMembership(UserOrganization membership) {
        if (membership.getStatus() != UserOrganizationStatus.ACTIVE) {
            return false;
        }

        if (membership.getRole() == null || membership.getRole().getCode() != RoleCode.SUPER_ADMIN) {
            return false;
        }

        Organization organization = membership.getOrganization();
        return organization != null
                && organization.getDeletedAt() == null
                && organization.getStatus() == OrganizationStatus.ACTIVE;
    }

    private UserOrganizationMembershipResponse toResponse(UserOrganization membership) {
        Organization organization = membership.getOrganization();

        return new UserOrganizationMembershipResponse(
                organization.getId(),
                organization.getName(),
                buildLogoUrl(organization),
                membership.getRole().getCode(),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
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
