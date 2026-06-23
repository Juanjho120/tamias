package com.tamias.organization.mapper;

import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.dto.OrganizationResponse;
import com.tamias.organization.entity.Organization;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    private final FileStorageService fileStorageService;

    public OrganizationMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getDescription(),
                organization.getStatus(),
                buildLogoUrl(organization),
                organization.getLogoOriginalFilename(),
                organization.getLogoContentType(),
                organization.getLogoSizeBytes(),
                organization.getLogoUpdatedAt(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()
        );
    }

    private String buildLogoUrl(Organization organization) {
        String logoS3Key = organization.getLogoS3Key();
        if (logoS3Key == null || logoS3Key.isBlank()) {
            return null;
        }

        return fileStorageService.buildFileUrl(logoS3Key);
    }
}
