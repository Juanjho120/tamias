package com.tamias.user.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.user.dto.UserCreateRequest;
import com.tamias.user.dto.UserOrganizationMembershipCreateRequest;
import com.tamias.user.dto.UserOrganizationMembershipResponse;
import com.tamias.user.dto.UserOrganizationMembershipUpdateRequest;
import com.tamias.user.dto.UserResponse;
import com.tamias.user.dto.UserSummaryResponse;
import com.tamias.user.dto.UserUpdateRequest;
import com.tamias.user.service.UserOrganizationMembershipService;
import com.tamias.user.service.UserService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserOrganizationMembershipService userOrganizationMembershipService;

    public UserController(
            UserService userService,
            UserOrganizationMembershipService userOrganizationMembershipService
    ) {
        this.userService = userService;
        this.userOrganizationMembershipService = userOrganizationMembershipService;
    }

    @GetMapping
    public PageResponse<UserSummaryResponse> findAll(Pageable pageable) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        userService.delete(id);
    }

    @GetMapping("/{id}/organizations")
    public List<UserOrganizationMembershipResponse> findOrganizations(@PathVariable UUID id) {
        return userOrganizationMembershipService.findAll(id);
    }

    @PostMapping("/{id}/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    public UserOrganizationMembershipResponse createOrganizationMembership(
            @PathVariable UUID id,
            @Valid @RequestBody UserOrganizationMembershipCreateRequest request
    ) {
        return userOrganizationMembershipService.create(id, request);
    }

    @PutMapping("/{id}/organizations/{organizationId}")
    public UserOrganizationMembershipResponse updateOrganizationMembership(
            @PathVariable UUID id,
            @PathVariable UUID organizationId,
            @Valid @RequestBody UserOrganizationMembershipUpdateRequest request
    ) {
        return userOrganizationMembershipService.update(id, organizationId, request);
    }

    @DeleteMapping("/{id}/organizations/{organizationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrganizationMembership(
            @PathVariable UUID id,
            @PathVariable UUID organizationId
    ) {
        userOrganizationMembershipService.delete(id, organizationId);
    }
}
