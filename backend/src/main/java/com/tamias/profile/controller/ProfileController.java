package com.tamias.profile.controller;

import com.tamias.auth.dto.AuthUserResponse;
import com.tamias.profile.dto.ChangePasswordRequest;
import com.tamias.profile.dto.ProfileUpdateRequest;
import com.tamias.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public AuthUserResponse getCurrentProfile() {
        return profileService.getCurrentProfile();
    }

    @PatchMapping
    public AuthUserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }

    @PatchMapping("/password")
    public AuthUserResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return profileService.changePassword(request);
    }
}
