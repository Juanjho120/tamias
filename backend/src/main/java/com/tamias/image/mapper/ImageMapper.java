package com.tamias.image.mapper;

import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.inventoryitem.entity.InventoryItemImage;
import com.tamias.image.maintenance.entity.MaintenanceRecordImage;
import com.tamias.image.property.entity.PropertyImage;
import org.springframework.stereotype.Component;

@Component
public class ImageMapper {

  private final FileStorageService fileStorageService;

  public ImageMapper(FileStorageService fileStorageService) {
    this.fileStorageService = fileStorageService;
  }

  public ImageResponse toResponse(PropertyImage entity) {
    return new ImageResponse(
        entity.getId(),
        entity.getProperty().getId(),
        entity.getOriginalFilename(),
        entity.getS3Key(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getCover(),
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }

  public ImageUploadResponse toUploadResponse(PropertyImage entity) {
    return new ImageUploadResponse(
        entity.getId(),
        entity.getProperty().getId(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getCover(),
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }

  public ImageResponse toResponse(MaintenanceRecordImage entity) {
    return new ImageResponse(
        entity.getId(),
        entity.getMaintenanceRecord().getId(),
        entity.getOriginalFilename(),
        entity.getS3Key(),
        entity.getContentType(),
        entity.getSizeBytes(),
        null,
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }

  public ImageUploadResponse toUploadResponse(MaintenanceRecordImage entity) {
    return new ImageUploadResponse(
        entity.getId(),
        entity.getMaintenanceRecord().getId(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        null,
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }

  public ImageResponse toResponse(InventoryItemImage entity) {
    return new ImageResponse(
        entity.getId(),
        entity.getInventoryItem().getId(),
        entity.getOriginalFilename(),
        entity.getS3Key(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getCover(),
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }

  public ImageUploadResponse toUploadResponse(InventoryItemImage entity) {
    return new ImageUploadResponse(
        entity.getId(),
        entity.getInventoryItem().getId(),
        entity.getOriginalFilename(),
        entity.getContentType(),
        entity.getSizeBytes(),
        entity.getCover(),
        entity.getStatus(),
        entity.getCreatedAt(),
        fileStorageService.buildFileUrl(entity.getS3Key()),
        fileStorageService.getDownloadUrlExpirationSeconds()
    );
  }
}
