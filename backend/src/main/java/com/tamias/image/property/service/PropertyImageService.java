package com.tamias.image.property.service;

import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.property.entity.PropertyImage;
import com.tamias.image.property.repository.PropertyImageRepository;
import com.tamias.image.service.ImageValidationService;
import com.tamias.property.entity.Property;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PropertyImageService {

    private final PropertyImageRepository propertyImageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ImageMapper imageMapper;

    public PropertyImageService(
            PropertyImageRepository propertyImageRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService,
            ImageValidationService imageValidationService,
            ImageMapper imageMapper
    ) {
        this.propertyImageRepository = propertyImageRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.imageMapper = imageMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ImageResponse> findAll(UUID propertyId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateProperty(propertyId, organizationId);

        return propertyImageRepository
                .findByProperty_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
                        propertyId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ImageResponse findById(UUID propertyId, UUID imageId) {
        return imageMapper.toResponse(findImage(propertyId, imageId));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ImageUploadResponse upload(UUID propertyId, MultipartFile file, Boolean cover) {
        imageValidationService.validateImage(file);

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Property property = validateProperty(propertyId, organizationId);
        User currentUser = getCurrentUser();

        boolean shouldBeCover = Boolean.TRUE.equals(cover)
                || propertyImageRepository.countByProperty_IdAndOrganization_IdAndStatus(
                propertyId,
                organizationId,
                ImageStatus.ACTIVE
        ) == 0;

        if (shouldBeCover) {
            clearCover(propertyId, organizationId);
        }

        var storedFile = fileStorageService.store(file, "properties/" + property.getId());

        PropertyImage entity = new PropertyImage();
        entity.setOrganization(property.getOrganization());
        entity.setProperty(property);
        entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setCover(shouldBeCover);
        entity.setStatus(ImageStatus.ACTIVE);
        entity.setCreatedBy(currentUser);

        return imageMapper.toUploadResponse(propertyImageRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ImageResponse setCover(UUID propertyId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        PropertyImage image = findImage(propertyId, imageId);

        clearCover(propertyId, organizationId);
        image.setCover(true);

        return imageMapper.toResponse(propertyImageRepository.save(image));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID propertyId, UUID imageId) {
        PropertyImage image = findImage(propertyId, imageId);
        return fileStorageService.loadAsResource(image.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID propertyId, UUID imageId) {
        PropertyImage image = findImage(propertyId, imageId);
        return MediaType.parseMediaType(image.getContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID propertyId, UUID imageId) {
        PropertyImage image = findImage(propertyId, imageId);

        fileStorageService.delete(image.getS3Key());
        propertyImageRepository.delete(image);
    }

    private Property validateProperty(UUID propertyId, UUID organizationId) {
        return propertyRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(propertyId, organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));
    }

    private PropertyImage findImage(UUID propertyId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return propertyImageRepository
                .findByIdAndProperty_IdAndOrganization_IdAndStatus(
                        imageId,
                        propertyId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("Property image not found"));
    }

    private void clearCover(UUID propertyId, UUID organizationId) {
        List<PropertyImage> coverImages = propertyImageRepository
                .findByProperty_IdAndOrganization_IdAndCoverAndStatus(
                        propertyId,
                        organizationId,
                        true,
                        ImageStatus.ACTIVE
                );

        for (PropertyImage coverImage : coverImages) {
            coverImage.setCover(false);
        }
        propertyImageRepository.saveAll(coverImages);
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
