package com.tamias.organization.controller;

import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/current")
    public OrganizationResponse getCurrentOrganization() {
        return organizationService.getCurrentOrganization();
    }

    @PutMapping("/current")
    public OrganizationResponse updateCurrentOrganization(
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        return organizationService.updateCurrentOrganization(request);
    }
}
