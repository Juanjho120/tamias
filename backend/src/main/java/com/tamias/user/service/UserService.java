package com.tamias.user.service;

import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.dto.UserCreateRequest;
import com.tamias.user.dto.UserResponse;
import com.tamias.user.dto.UserSummaryResponse;
import com.tamias.user.dto.UserUpdateRequest;
import com.tamias.user.entity.User;
import com.tamias.user.entity.UserOrganization;
import com.tamias.user.enums.UserOrganizationStatus;
import com.tamias.user.enums.UserStatus;
import com.tamias.user.mapper.UserMapper;
import com.tamias.user.repository.RoleRepository;
import com.tamias.user.repository.UserOrganizationRepository;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        UserOrganizationRepository userOrganizationRepository,
        OrganizationRepository organizationRepository,
        CurrentUserService currentUserService,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.organizationRepository = organizationRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> findAll(Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var page = userOrganizationRepository
            .findByOrganization_IdAndStatus(
                organizationId,
                UserOrganizationStatus.ACTIVE,
                pageable
            )
            .map(userMapper::toSummaryResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        UserOrganization userOrganization = findUserOrganizationInCurrentOrganization(id);
        return userMapper.toResponse(userOrganization);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        var role = roleRepository.findByCode(request.role())
            .orElseThrow(() -> new BadRequestException("Invalid role"));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangeRequired(true);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);

        UserOrganization userOrganization = new UserOrganization();
        userOrganization.setUser(user);
        userOrganization.setOrganization(organization);
        userOrganization.setRole(role);
        userOrganization.setStatus(UserOrganizationStatus.ACTIVE);

        userOrganization = userOrganizationRepository.save(userOrganization);

        return userMapper.toResponse(userOrganization);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        UserOrganization userOrganization = findUserOrganizationInCurrentOrganization(id);
        User user = userOrganization.getUser();

        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        var role = roleRepository.findByCode(request.role())
            .orElseThrow(() -> new BadRequestException("Invalid role"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setStatus(request.status());
        userOrganization.setRole(role);

        userRepository.save(user);
        userOrganization = userOrganizationRepository.save(userOrganization);

        return userMapper.toResponse(userOrganization);
    }

    @Transactional
    public void delete(UUID id) {
        if (currentUserService.getCurrentUserId().equals(id)) {
            throw new BadRequestException("You cannot delete your own user");
        }

        UserOrganization userOrganization = findUserOrganizationInCurrentOrganization(id);
        User user = userOrganization.getUser();

        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(OffsetDateTime.now());
        userOrganization.setStatus(UserOrganizationStatus.DELETED);

        userRepository.save(user);
        userOrganizationRepository.save(userOrganization);
    }

    private UserOrganization findUserOrganizationInCurrentOrganization(UUID userId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        UserOrganization userOrganization = userOrganizationRepository
            .findByUser_IdAndOrganization_Id(userId, organizationId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        if (userOrganization.getStatus() == UserOrganizationStatus.DELETED || userOrganization.getUser().getDeletedAt() != null) {
            throw new NotFoundException("User not found");
        }

        return userOrganization;
    }
}
