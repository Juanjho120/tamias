package com.tamias.organization.service;

import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.mapper.OrganizationMapper;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final CurrentUserService currentUserService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMapper organizationMapper,
            CurrentUserService currentUserService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        Organization organization = organizationRepository
                .findByIdAndDeletedAtIsNull(currentUserService.getCurrentOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        return organizationMapper.toResponse(organization);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public OrganizationResponse updateCurrentOrganization(OrganizationUpdateRequest request) {
        Organization organization = organizationRepository
                .findByIdAndDeletedAtIsNull(currentUserService.getCurrentOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (!organization.getName().equalsIgnoreCase(request.name())
                && organizationRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Organization name already exists");
        }

        organization.setName(request.name());
        organization.setDescription(request.description());

        return organizationMapper.toResponse(organizationRepository.save(organization));
    }
}
