package com.tamias.organization.service;

import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.document.storage.StoredFile;
import com.tamias.image.service.ImageValidationService;
import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.mapper.OrganizationMapper;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import java.time.OffsetDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;

    public OrganizationService(
            OrganizationRepository organizationRepository,
            OrganizationMapper organizationMapper,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService,
            ImageValidationService imageValidationService
    ) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        Organization organization = findCurrentOrganization();
        return organizationMapper.toResponse(organization);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public OrganizationResponse updateCurrentOrganization(OrganizationUpdateRequest request) {
        Organization organization = findCurrentOrganization();

        if (!organization.getName().equalsIgnoreCase(request.name())
                && organizationRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Organization name already exists");
        }

        organization.setName(request.name());
        organization.setDescription(request.description());

        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public OrganizationResponse uploadCurrentOrganizationLogo(MultipartFile file) {
        imageValidationService.validateImage(file);

        Organization organization = findCurrentOrganization();
        String previousLogoS3Key = organization.getLogoS3Key();

        StoredFile storedFile = fileStorageService.store(file, buildLogoStorageFolder(organization));

        organization.setLogoOriginalFilename(normalizeOriginalFilename(file));
        organization.setLogoS3Key(storedFile.storageKey());
        organization.setLogoFilepath(storedFile.filepath());
        organization.setLogoContentType(storedFile.contentType());
        organization.setLogoSizeBytes(storedFile.sizeBytes());
        organization.setLogoUpdatedAt(OffsetDateTime.now());

        Organization savedOrganization = organizationRepository.save(organization);
        deletePreviousLogo(previousLogoS3Key, storedFile.storageKey());

        return organizationMapper.toResponse(savedOrganization);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public OrganizationResponse deleteCurrentOrganizationLogo() {
        Organization organization = findCurrentOrganization();
        String previousLogoS3Key = organization.getLogoS3Key();

        organization.setLogoOriginalFilename(null);
        organization.setLogoS3Key(null);
        organization.setLogoFilepath(null);
        organization.setLogoContentType(null);
        organization.setLogoSizeBytes(null);
        organization.setLogoUpdatedAt(null);

        Organization savedOrganization = organizationRepository.save(organization);
        deleteLogoIfPresent(previousLogoS3Key);

        return organizationMapper.toResponse(savedOrganization);
    }

    private Organization findCurrentOrganization() {
        return organizationRepository
                .findByIdAndDeletedAtIsNull(currentUserService.getCurrentOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private String buildLogoStorageFolder(Organization organization) {
        return organization.getId() + "/organization/logo";
    }

    private String normalizeOriginalFilename(MultipartFile file) {
        return file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "organization-logo";
    }

    private void deletePreviousLogo(String previousLogoS3Key, String newLogoS3Key) {
        if (previousLogoS3Key == null || previousLogoS3Key.isBlank()) {
            return;
        }

        if (previousLogoS3Key.equals(newLogoS3Key)) {
            return;
        }

        fileStorageService.delete(previousLogoS3Key);
    }

    private void deleteLogoIfPresent(String logoS3Key) {
        if (logoS3Key == null || logoS3Key.isBlank()) {
            return;
        }

        fileStorageService.delete(logoS3Key);
    }
}
