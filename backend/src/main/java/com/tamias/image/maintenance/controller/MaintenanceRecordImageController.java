package com.tamias.image.maintenance.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.maintenance.service.MaintenanceRecordImageService;
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
@RequestMapping("/api/v1/maintenance-records/{maintenanceRecordId}/images")
public class MaintenanceRecordImageController {

    private final MaintenanceRecordImageService maintenanceRecordImageService;

    public MaintenanceRecordImageController(MaintenanceRecordImageService maintenanceRecordImageService) {
        this.maintenanceRecordImageService = maintenanceRecordImageService;
    }

    @GetMapping
    public List<ImageResponse> findAll(@PathVariable UUID maintenanceRecordId) {
        return maintenanceRecordImageService.findAll(maintenanceRecordId);
    }

    @GetMapping("/{imageId}")
    public ImageResponse findById(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID imageId
    ) {
        return maintenanceRecordImageService.findById(maintenanceRecordId, imageId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(
            @PathVariable UUID maintenanceRecordId,
            @RequestPart("file") MultipartFile file
    ) {
        return maintenanceRecordImageService.upload(maintenanceRecordId, file);
    }

    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> getFile(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID imageId
    ) {
        Resource resource = maintenanceRecordImageService.getFile(maintenanceRecordId, imageId);

        return ResponseEntity.ok()
                .contentType(maintenanceRecordImageService.getMediaType(maintenanceRecordId, imageId))
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
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID imageId
    ) {
        maintenanceRecordImageService.delete(maintenanceRecordId, imageId);
    }
}
