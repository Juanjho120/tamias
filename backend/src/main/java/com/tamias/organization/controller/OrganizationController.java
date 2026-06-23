package com.tamias.organization.controller;

import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @RequestMapping(
            value = "/current/logo",
            method = { RequestMethod.POST, RequestMethod.PUT },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public OrganizationResponse uploadCurrentOrganizationLogo(@RequestPart("file") MultipartFile file) {
        return organizationService.uploadCurrentOrganizationLogo(file);
    }

    @DeleteMapping("/current/logo")
    public OrganizationResponse deleteCurrentOrganizationLogo() {
        return organizationService.deleteCurrentOrganizationLogo();
    }
}
