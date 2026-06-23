package com.tamias.auth.controller;

import com.tamias.auth.dto.AuthOrganizationOptionResponse;
import com.tamias.auth.dto.LoginRequest;
import com.tamias.auth.dto.LoginResponse;
import com.tamias.auth.dto.SwitchOrganizationRequest;
import com.tamias.auth.service.AuthService;
import com.tamias.security.service.CurrentUserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public LoginResponse me() {
        return authService.getCurrentUserResponse(
                currentUserService.getCurrentUserId(),
                currentUserService.getCurrentOrganizationId()
        );
    }

    @GetMapping("/organizations")
    public List<AuthOrganizationOptionResponse> organizations() {
        return authService.findAvailableOrganizations(
                currentUserService.getCurrentUserId(),
                currentUserService.getCurrentOrganizationId()
        );
    }

    @PostMapping("/switch-organization")
    public LoginResponse switchOrganization(@Valid @RequestBody SwitchOrganizationRequest request) {
        return authService.switchOrganization(currentUserService.getCurrentUserId(), request);
    }
}
