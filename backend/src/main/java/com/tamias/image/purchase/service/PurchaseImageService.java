package com.tamias.image.purchase.service;

import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.purchase.entity.PurchaseImage;
import com.tamias.image.purchase.repository.PurchaseImageRepository;
import com.tamias.image.service.ImageValidationService;
import com.tamias.purchase.entity.PurchaseList;
import com.tamias.purchase.repository.PurchaseListRepository;
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
public class PurchaseImageService {

    private final PurchaseImageRepository purchaseImageRepository;
    private final PurchaseListRepository purchaseListRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ImageMapper imageMapper;

    public PurchaseImageService(
        PurchaseImageRepository purchaseImageRepository,
        PurchaseListRepository purchaseListRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        ImageValidationService imageValidationService,
        ImageMapper imageMapper
    ) {
        this.purchaseImageRepository = purchaseImageRepository;
        this.purchaseListRepository = purchaseListRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.imageMapper = imageMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ImageResponse> findAll(UUID purchaseListId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validatePurchaseList(purchaseListId, organizationId);
        return purchaseImageRepository
            .findByPurchaseList_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
                purchaseListId,
                organizationId,
                ImageStatus.ACTIVE
            )
            .stream()
            .map(imageMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ImageResponse findById(UUID purchaseListId, UUID imageId) {
        return imageMapper.toResponse(findImage(purchaseListId, imageId));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ImageUploadResponse upload(UUID purchaseListId, MultipartFile file) {
        imageValidationService.validateImage(file);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        PurchaseList purchaseList = validatePurchaseList(purchaseListId, organizationId);
        User currentUser = getCurrentUser();

        var storedFile = fileStorageService.store(
            file,
            purchaseList.getOrganization().getId() + "/purchases/" + purchaseList.getId()
        );

        PurchaseImage entity = new PurchaseImage();
        entity.setOrganization(purchaseList.getOrganization());
        entity.setPurchaseList(purchaseList);
        entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setStatus(ImageStatus.ACTIVE);
        entity.setCreatedBy(currentUser);

        return imageMapper.toUploadResponse(purchaseImageRepository.save(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID purchaseListId, UUID imageId) {
        PurchaseImage image = findImage(purchaseListId, imageId);
        return fileStorageService.loadAsResource(image.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID purchaseListId, UUID imageId) {
        PurchaseImage image = findImage(purchaseListId, imageId);
        return MediaType.parseMediaType(image.getContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(UUID purchaseListId, UUID imageId) {
        PurchaseImage image = findImage(purchaseListId, imageId);
        fileStorageService.delete(image.getS3Key());
        purchaseImageRepository.delete(image);
    }

    private PurchaseList validatePurchaseList(UUID purchaseListId, UUID organizationId) {
        return purchaseListRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(purchaseListId, organizationId)
            .orElseThrow(() -> new NotFoundException("Purchase list not found"));
    }

    private PurchaseImage findImage(UUID purchaseListId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return purchaseImageRepository
            .findByIdAndPurchaseList_IdAndOrganization_IdAndStatus(
                imageId,
                purchaseListId,
                organizationId,
                ImageStatus.ACTIVE
            )
            .orElseThrow(() -> new NotFoundException("Purchase image not found"));
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
