package com.tamias.image.maintenance.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.maintenance.dto.MaintenanceRecordImageRoleRequest;
import com.tamias.image.maintenance.enums.MaintenanceImageRole;
import com.tamias.image.maintenance.service.MaintenanceRecordImageService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "imageRole", required = false, defaultValue = "GENERAL") MaintenanceImageRole imageRole
    ) {
        return maintenanceRecordImageService.upload(maintenanceRecordId, file, imageRole);
    }

    @PatchMapping("/{imageId}/role")
    public ImageResponse updateRole(
            @PathVariable UUID maintenanceRecordId,
            @PathVariable UUID imageId,
            @Valid @RequestBody MaintenanceRecordImageRoleRequest request
    ) {
        return maintenanceRecordImageService.updateRole(maintenanceRecordId, imageId, request.imageRole());
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
