package com.tamias.image.inventoryitem.service;

import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.inventoryitem.entity.InventoryItemImage;
import com.tamias.image.inventoryitem.repository.InventoryItemImageRepository;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.service.ImageValidationService;
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
public class InventoryItemImageService {

  private final InventoryItemImageRepository inventoryItemImageRepository;
  private final InventoryItemRepository inventoryItemRepository;
  private final UserRepository userRepository;
  private final CurrentUserService currentUserService;
  private final FileStorageService fileStorageService;
  private final ImageValidationService imageValidationService;
  private final ImageMapper imageMapper;

  public InventoryItemImageService(
      InventoryItemImageRepository inventoryItemImageRepository,
      InventoryItemRepository inventoryItemRepository,
      UserRepository userRepository,
      CurrentUserService currentUserService,
      FileStorageService fileStorageService,
      ImageValidationService imageValidationService,
      ImageMapper imageMapper
  ) {
    this.inventoryItemImageRepository = inventoryItemImageRepository;
    this.inventoryItemRepository = inventoryItemRepository;
    this.userRepository = userRepository;
    this.currentUserService = currentUserService;
    this.fileStorageService = fileStorageService;
    this.imageValidationService = imageValidationService;
    this.imageMapper = imageMapper;
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
  public List<ImageResponse> findAll(UUID inventoryItemId) {
    UUID organizationId = currentUserService.getCurrentOrganizationId();
    validateInventoryItem(inventoryItemId, organizationId);

    return inventoryItemImageRepository
        .findByInventoryItem_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
            inventoryItemId,
            organizationId,
            ImageStatus.ACTIVE
        )
        .stream()
        .map(imageMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
  public ImageResponse findById(UUID inventoryItemId, UUID imageId) {
    return imageMapper.toResponse(findImage(inventoryItemId, imageId));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
  public ImageUploadResponse upload(UUID inventoryItemId, MultipartFile file, Boolean cover) {
    imageValidationService.validateImage(file);

    UUID organizationId = currentUserService.getCurrentOrganizationId();
    InventoryItem inventoryItem = validateInventoryItem(inventoryItemId, organizationId);
    User currentUser = getCurrentUser();

    boolean shouldBeCover = Boolean.TRUE.equals(cover)
        || inventoryItemImageRepository.countByInventoryItem_IdAndOrganization_IdAndStatus(
            inventoryItemId,
            organizationId,
            ImageStatus.ACTIVE
        ) == 0;

    if (shouldBeCover) {
      clearCover(inventoryItemId, organizationId);
    }

    var storedFile = fileStorageService.store(
        file,
        inventoryItem.getOrganization().getId() + "/catalogs/inventory_items/" + inventoryItem.getId()
    );

    InventoryItemImage entity = new InventoryItemImage();
    entity.setOrganization(inventoryItem.getOrganization());
    entity.setInventoryItem(inventoryItem);
    entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
    entity.setS3Key(storedFile.storageKey());
    entity.setFilepath(storedFile.filepath());
    entity.setContentType(storedFile.contentType());
    entity.setSizeBytes(storedFile.sizeBytes());
    entity.setCover(shouldBeCover);
    entity.setStatus(ImageStatus.ACTIVE);
    entity.setCreatedBy(currentUser);

    return imageMapper.toUploadResponse(inventoryItemImageRepository.save(entity));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
  public ImageResponse setCover(UUID inventoryItemId, UUID imageId) {
    UUID organizationId = currentUserService.getCurrentOrganizationId();
    InventoryItemImage image = findImage(inventoryItemId, imageId);

    clearCover(inventoryItemId, organizationId);
    image.setCover(true);

    return imageMapper.toResponse(inventoryItemImageRepository.save(image));
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
  public Resource getFile(UUID inventoryItemId, UUID imageId) {
    InventoryItemImage image = findImage(inventoryItemId, imageId);
    return fileStorageService.loadAsResource(image.getS3Key());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
  public MediaType getMediaType(UUID inventoryItemId, UUID imageId) {
    InventoryItemImage image = findImage(inventoryItemId, imageId);
    return MediaType.parseMediaType(image.getContentType());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
  public void delete(UUID inventoryItemId, UUID imageId) {
    InventoryItemImage image = findImage(inventoryItemId, imageId);

    fileStorageService.delete(image.getS3Key());
    inventoryItemImageRepository.delete(image);
  }

  private InventoryItem validateInventoryItem(UUID inventoryItemId, UUID organizationId) {
    return inventoryItemRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(inventoryItemId, organizationId)
        .orElseThrow(() -> new NotFoundException("Inventory item not found"));
  }

  private InventoryItemImage findImage(UUID inventoryItemId, UUID imageId) {
    UUID organizationId = currentUserService.getCurrentOrganizationId();

    return inventoryItemImageRepository
        .findByIdAndInventoryItem_IdAndOrganization_IdAndStatus(
            imageId,
            inventoryItemId,
            organizationId,
            ImageStatus.ACTIVE
        )
        .orElseThrow(() -> new NotFoundException("Inventory item image not found"));
  }

  private void clearCover(UUID inventoryItemId, UUID organizationId) {
    List<InventoryItemImage> coverImages = inventoryItemImageRepository
        .findByInventoryItem_IdAndOrganization_IdAndCoverAndStatus(
            inventoryItemId,
            organizationId,
            true,
            ImageStatus.ACTIVE
        );

    for (InventoryItemImage coverImage : coverImages) {
      coverImage.setCover(false);
    }

    inventoryItemImageRepository.saveAll(coverImages);
  }

  private User getCurrentUser() {
    return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
