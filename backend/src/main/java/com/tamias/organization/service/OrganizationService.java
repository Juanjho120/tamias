package com.tamias.organization.service;

import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.document.storage.StoredFile;
import com.tamias.image.service.ImageValidationService;
import com.tamias.organization.dto.OrganizationCreateRequest;
import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.dto.OrganizationStatusUpdateRequest;
import com.tamias.organization.dto.OrganizationUpdateRequest;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.enums.OrganizationStatus;
import com.tamias.organization.mapper.OrganizationMapper;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.enums.RoleCode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public PageResponse<OrganizationResponse> findManagedOrganizations(Pageable pageable) {
        if (isSuperAdmin()) {
            Page<OrganizationResponse> page = organizationRepository
                    .findByDeletedAtIsNull(pageable)
                    .map(organizationMapper::toResponse);
            return PageResponse.from(page);
        }

        Organization organization = findCurrentOrganization();
        Page<OrganizationResponse> page = new PageImpl<>(
                List.of(organizationMapper.toResponse(organization)),
                pageable,
                1
        );
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse findManagedOrganizationById(UUID id) {
        return organizationMapper.toResponse(findManagedOrganization(id));
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrganizationResponse create(OrganizationCreateRequest request) {
        String normalizedName = normalizeName(request.name());
        if (organizationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Organization name already exists");
        }

        Organization organization = new Organization();
        organization.setName(normalizedName);
        organization.setDescription(request.description());
        organization.setStatus(OrganizationStatus.ACTIVE);
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse updateManagedOrganization(UUID id, OrganizationUpdateRequest request) {
        Organization organization = findManagedOrganization(id);
        return updateOrganization(organization, request);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public OrganizationResponse updateStatus(UUID id, OrganizationStatusUpdateRequest request) {
        if (request.status() == OrganizationStatus.DELETED) {
            throw new BadRequestException("Use delete semantics for deleted organizations");
        }

        Organization organization = organizationRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        organization.setStatus(request.status());
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse uploadOrganizationLogo(UUID id, MultipartFile file) {
        imageValidationService.validateImage(file);
        Organization organization = findManagedOrganization(id);
        return uploadLogo(organization, file);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse deleteOrganizationLogo(UUID id) {
        Organization organization = findManagedOrganization(id);
        return deleteLogo(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        Organization organization = findCurrentOrganization();
        return organizationMapper.toResponse(organization);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse updateCurrentOrganization(OrganizationUpdateRequest request) {
        Organization organization = findCurrentOrganization();
        return updateOrganization(organization, request);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse uploadCurrentOrganizationLogo(MultipartFile file) {
        imageValidationService.validateImage(file);
        Organization organization = findCurrentOrganization();
        return uploadLogo(organization, file);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
    public OrganizationResponse deleteCurrentOrganizationLogo() {
        Organization organization = findCurrentOrganization();
        return deleteLogo(organization);
    }

    private OrganizationResponse updateOrganization(Organization organization, OrganizationUpdateRequest request) {
        String normalizedName = normalizeName(request.name());
        if (!organization.getName().equalsIgnoreCase(normalizedName)
                && organizationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Organization name already exists");
        }

        organization.setName(normalizedName);
        organization.setDescription(request.description());
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    private OrganizationResponse uploadLogo(Organization organization, MultipartFile file) {
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

    private OrganizationResponse deleteLogo(Organization organization) {
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

    private Organization findManagedOrganization(UUID id) {
        if (isSuperAdmin()) {
            return organizationRepository
                    .findByIdAndDeletedAtIsNull(id)
                    .orElseThrow(() -> new NotFoundException("Organization not found"));
        }

        UUID currentOrganizationId = currentUserService.getCurrentOrganizationId();
        if (!currentOrganizationId.equals(id)) {
            throw new NotFoundException("Organization not found");
        }
        return findCurrentOrganization();
    }

    private Organization findCurrentOrganization() {
        return organizationRepository
                .findByIdAndDeletedAtIsNull(currentUserService.getCurrentOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    private boolean isSuperAdmin() {
        return RoleCode.SUPER_ADMIN.name().equals(currentUserService.getCurrentRole());
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
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
