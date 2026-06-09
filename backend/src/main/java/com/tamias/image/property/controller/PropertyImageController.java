package com.tamias.image.property.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.property.service.PropertyImageService;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/properties/{propertyId}/images")
public class PropertyImageController {

    private final PropertyImageService propertyImageService;

    public PropertyImageController(PropertyImageService propertyImageService) {
        this.propertyImageService = propertyImageService;
    }

    @GetMapping
    public List<ImageResponse> findAll(@PathVariable UUID propertyId) {
        return propertyImageService.findAll(propertyId);
    }

    @GetMapping("/{imageId}")
    public ImageResponse findById(
            @PathVariable UUID propertyId,
            @PathVariable UUID imageId
    ) {
        return propertyImageService.findById(propertyId, imageId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(
            @PathVariable UUID propertyId,
            @RequestParam(required = false) Boolean cover,
            @RequestPart("file") MultipartFile file
    ) {
        return propertyImageService.upload(propertyId, file, cover);
    }

    @PatchMapping("/{imageId}/cover")
    public ImageResponse setCover(
            @PathVariable UUID propertyId,
            @PathVariable UUID imageId
    ) {
        return propertyImageService.setCover(propertyId, imageId);
    }

    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> getFile(
            @PathVariable UUID propertyId,
            @PathVariable UUID imageId
    ) {
        Resource resource = propertyImageService.getFile(propertyId, imageId);

        return ResponseEntity.ok()
                .contentType(propertyImageService.getMediaType(propertyId, imageId))
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
            @PathVariable UUID propertyId,
            @PathVariable UUID imageId
    ) {
        propertyImageService.delete(propertyId, imageId);
    }
}
