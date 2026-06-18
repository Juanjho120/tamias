package com.tamias.image.purchase.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.purchase.service.PurchaseImageService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/purchase-lists/{purchaseListId}/images")
public class PurchaseImageController {

    private final PurchaseImageService purchaseImageService;

    public PurchaseImageController(PurchaseImageService purchaseImageService) {
        this.purchaseImageService = purchaseImageService;
    }

    @GetMapping
    public List<ImageResponse> findAll(@PathVariable UUID purchaseListId) {
        return purchaseImageService.findAll(purchaseListId);
    }

    @GetMapping("/{imageId}")
    public ImageResponse findById(
        @PathVariable UUID purchaseListId,
        @PathVariable UUID imageId
    ) {
        return purchaseImageService.findById(purchaseListId, imageId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(
        @PathVariable UUID purchaseListId,
        @RequestPart("file") MultipartFile file
    ) {
        return purchaseImageService.upload(purchaseListId, file);
    }

    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> getFile(
        @PathVariable UUID purchaseListId,
        @PathVariable UUID imageId
    ) {
        Resource resource = purchaseImageService.getFile(purchaseListId, imageId);
        return ResponseEntity.ok()
            .contentType(purchaseImageService.getMediaType(purchaseListId, imageId))
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
        @PathVariable UUID purchaseListId,
        @PathVariable UUID imageId
    ) {
        purchaseImageService.delete(purchaseListId, imageId);
    }
}
