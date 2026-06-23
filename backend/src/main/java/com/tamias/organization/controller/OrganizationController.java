package com.tamias.organization.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.organization.dto.OrganizationCreateRequest;
import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationStatusUpdateRequest;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.service.OrganizationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public PageResponse<OrganizationResponse> findManagedOrganizations(Pageable pageable) {
        return organizationService.findManagedOrganizations(pageable);
    }

    @GetMapping("/{id}")
    public OrganizationResponse findManagedOrganizationById(@PathVariable UUID id) {
        return organizationService.findManagedOrganizationById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody OrganizationCreateRequest request) {
        return organizationService.create(request);
    }

    @PutMapping("/{id}")
    public OrganizationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        return organizationService.updateManagedOrganization(id, request);
    }

    @PatchMapping("/{id}/status")
    public OrganizationResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationStatusUpdateRequest request
    ) {
        return organizationService.updateStatus(id, request);
    }

    @RequestMapping(
            value = "/{id}/logo",
            method = { RequestMethod.POST, RequestMethod.PUT },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public OrganizationResponse uploadLogo(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file
    ) {
        return organizationService.uploadOrganizationLogo(id, file);
    }

    @DeleteMapping("/{id}/logo")
    public OrganizationResponse deleteLogo(@PathVariable UUID id) {
        return organizationService.deleteOrganizationLogo(id);
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
