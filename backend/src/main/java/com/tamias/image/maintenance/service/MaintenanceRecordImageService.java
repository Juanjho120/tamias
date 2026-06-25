package com.tamias.image.maintenance.service;

import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.maintenance.entity.MaintenanceRecordImage;
import com.tamias.image.maintenance.enums.MaintenanceImageRole;
import com.tamias.image.maintenance.repository.MaintenanceRecordImageRepository;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.service.ImageValidationService;
import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.maintenance.repository.MaintenanceRecordRepository;
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
public class MaintenanceRecordImageService {

    private final MaintenanceRecordImageRepository imageRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ImageMapper imageMapper;

    public MaintenanceRecordImageService(
            MaintenanceRecordImageRepository imageRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService,
            ImageValidationService imageValidationService,
            ImageMapper imageMapper
    ) {
        this.imageRepository = imageRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.imageMapper = imageMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ImageResponse> findAll(UUID maintenanceRecordId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateMaintenanceRecord(maintenanceRecordId, organizationId);

        return imageRepository
                .findByMaintenanceRecord_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
                        maintenanceRecordId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ImageResponse findById(UUID maintenanceRecordId, UUID imageId) {
        return imageMapper.toResponse(findImage(maintenanceRecordId, imageId));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ImageUploadResponse upload(
            UUID maintenanceRecordId,
            MultipartFile file,
            MaintenanceImageRole imageRole
    ) {
        imageValidationService.validateImage(file);

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        MaintenanceRecord maintenanceRecord = validateMaintenanceRecord(maintenanceRecordId, organizationId);
        User currentUser = getCurrentUser();

        var storedFile = fileStorageService.store(
                file,
                maintenanceRecord.getOrganization().getId() + "/maintenance/" + maintenanceRecord.getId()
        );

        MaintenanceRecordImage entity = new MaintenanceRecordImage();
        entity.setOrganization(maintenanceRecord.getOrganization());
        entity.setMaintenanceRecord(maintenanceRecord);
        entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setImageRole(imageRole != null ? imageRole : MaintenanceImageRole.GENERAL);
        entity.setStatus(ImageStatus.ACTIVE);
        entity.setCreatedBy(currentUser);

        return imageMapper.toUploadResponse(imageRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ImageResponse updateRole(
            UUID maintenanceRecordId,
            UUID imageId,
            MaintenanceImageRole imageRole
    ) {
        MaintenanceRecordImage image = findImage(maintenanceRecordId, imageId);
        image.setImageRole(imageRole != null ? imageRole : MaintenanceImageRole.GENERAL);
        return imageMapper.toResponse(imageRepository.save(image));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID maintenanceRecordId, UUID imageId) {
        MaintenanceRecordImage image = findImage(maintenanceRecordId, imageId);
        return fileStorageService.loadAsResource(image.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID maintenanceRecordId, UUID imageId) {
        MaintenanceRecordImage image = findImage(maintenanceRecordId, imageId);
        return MediaType.parseMediaType(image.getContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(UUID maintenanceRecordId, UUID imageId) {
        MaintenanceRecordImage image = findImage(maintenanceRecordId, imageId);
        fileStorageService.delete(image.getS3Key());
        imageRepository.delete(image);
    }

    private MaintenanceRecord validateMaintenanceRecord(UUID maintenanceRecordId, UUID organizationId) {
        return maintenanceRecordRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(maintenanceRecordId, organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance record not found"));
    }

    private MaintenanceRecordImage findImage(UUID maintenanceRecordId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return imageRepository
                .findByIdAndMaintenanceRecord_IdAndOrganization_IdAndStatus(
                        imageId,
                        maintenanceRecordId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("Maintenance record image not found"));
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
