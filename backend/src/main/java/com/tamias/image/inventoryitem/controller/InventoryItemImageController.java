package com.tamias.image.inventoryitem.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.inventoryitem.service.InventoryItemImageService;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inventory-items/{inventoryItemId}/images")
public class InventoryItemImageController {

  private final InventoryItemImageService inventoryItemImageService;

  public InventoryItemImageController(InventoryItemImageService inventoryItemImageService) {
    this.inventoryItemImageService = inventoryItemImageService;
  }

  @GetMapping
  public List<ImageResponse> findAll(@PathVariable UUID inventoryItemId) {
    return inventoryItemImageService.findAll(inventoryItemId);
  }

  @GetMapping("/{imageId}")
  public ImageResponse findById(
      @PathVariable UUID inventoryItemId,
      @PathVariable UUID imageId
  ) {
    return inventoryItemImageService.findById(inventoryItemId, imageId);
  }

  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public ImageUploadResponse upload(
      @PathVariable UUID inventoryItemId,
      @RequestParam(required = false) Boolean cover,
      @RequestPart("file") MultipartFile file
  ) {
    return inventoryItemImageService.upload(inventoryItemId, file, cover);
  }

  @PatchMapping("/{imageId}/cover")
  public ImageResponse setCover(
      @PathVariable UUID inventoryItemId,
      @PathVariable UUID imageId
  ) {
    return inventoryItemImageService.setCover(inventoryItemId, imageId);
  }

  @GetMapping("/{imageId}/file")
  public ResponseEntity<Resource> getFile(
      @PathVariable UUID inventoryItemId,
      @PathVariable UUID imageId
  ) {
    Resource resource = inventoryItemImageService.getFile(inventoryItemId, imageId);

    return ResponseEntity.ok()
        .contentType(inventoryItemImageService.getMediaType(inventoryItemId, imageId))
        .cacheControl(CacheControl.noCache())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(resource.getFilename() != null ? resource.getFilename() : "image")
                .build()
                .toString()
        )
        .body(resource);
  }

  @DeleteMapping("/{imageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable UUID inventoryItemId,
      @PathVariable UUID imageId
  ) {
    inventoryItemImageService.delete(inventoryItemId, imageId);
  }
}
