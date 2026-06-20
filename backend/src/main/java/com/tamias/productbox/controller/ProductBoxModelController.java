package com.tamias.productbox.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.productbox.dto.ProductBoxModelFaceResponse;
import com.tamias.productbox.dto.ProductBoxModelRequest;
import com.tamias.productbox.dto.ProductBoxModelResponse;
import com.tamias.productbox.dto.ProductBoxTextureProcessRequest;
import com.tamias.productbox.service.ProductBoxModelFaceService;
import com.tamias.productbox.service.ProductBoxModelService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/product-box-models")
public class ProductBoxModelController {

    private final ProductBoxModelService productBoxModelService;
    private final ProductBoxModelFaceService productBoxModelFaceService;

    public ProductBoxModelController(
        ProductBoxModelService productBoxModelService,
        ProductBoxModelFaceService productBoxModelFaceService
    ) {
        this.productBoxModelService = productBoxModelService;
        this.productBoxModelFaceService = productBoxModelFaceService;
    }

    @GetMapping
    public PageResponse<ProductBoxModelResponse> findAll(
        @RequestParam(required = false) UUID inventoryItemId,
        @RequestParam(required = false) UUID purchaseItemId,
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        return productBoxModelService.findAll(inventoryItemId, purchaseItemId, search, pageable);
    }

    @GetMapping("/{id}")
    public ProductBoxModelResponse findById(@PathVariable UUID id) {
        return productBoxModelService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBoxModelResponse create(@Valid @RequestBody ProductBoxModelRequest request) {
        return productBoxModelService.create(request);
    }

    @PutMapping("/{id}")
    public ProductBoxModelResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody ProductBoxModelRequest request
    ) {
        return productBoxModelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productBoxModelService.delete(id);
    }

    @GetMapping("/{id}/faces")
    public List<ProductBoxModelFaceResponse> findFaces(@PathVariable UUID id) {
        return productBoxModelFaceService.findAll(id);
    }

    @GetMapping("/{id}/faces/{faceName}")
    public ProductBoxModelFaceResponse findFace(
        @PathVariable UUID id,
        @PathVariable String faceName
    ) {
        return productBoxModelFaceService.findByFaceName(id, faceName);
    }

    @PostMapping(value = "/{id}/faces/{faceName}/texture/original", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBoxModelFaceResponse uploadOriginalFaceTexture(
        @PathVariable UUID id,
        @PathVariable String faceName,
        @RequestPart("file") MultipartFile file
    ) {
        return productBoxModelFaceService.uploadOriginal(id, faceName, file);
    }

    @PostMapping("/{id}/faces/{faceName}/texture/process")
    public ProductBoxModelFaceResponse processFaceTexture(
        @PathVariable UUID id,
        @PathVariable String faceName,
        @Valid @RequestBody ProductBoxTextureProcessRequest request
    ) {
        return productBoxModelFaceService.processTexture(id, faceName, request);
    }

    @GetMapping("/{id}/faces/{faceName}/texture/original/file")
    public ResponseEntity<Resource> getOriginalFaceFile(
        @PathVariable UUID id,
        @PathVariable String faceName
    ) {
        Resource resource = productBoxModelFaceService.getOriginalFile(id, faceName);
        return ResponseEntity.ok()
            .contentType(productBoxModelFaceService.getOriginalMediaType(id, faceName))
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

    @PostMapping(value = "/{id}/faces/{faceName}", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBoxModelFaceResponse uploadFace(
        @PathVariable UUID id,
        @PathVariable String faceName,
        @RequestParam(required = false) BigDecimal rotationDegrees,
        @RequestParam(required = false) Boolean flipHorizontal,
        @RequestParam(required = false) Boolean flipVertical,
        @RequestPart("file") MultipartFile file
    ) {
        return productBoxModelFaceService.uploadOrReplace(
            id,
            faceName,
            file,
            rotationDegrees,
            flipHorizontal,
            flipVertical
        );
    }

    @PutMapping(value = "/{id}/faces/{faceName}", consumes = "multipart/form-data")
    public ProductBoxModelFaceResponse replaceFace(
        @PathVariable UUID id,
        @PathVariable String faceName,
        @RequestParam(required = false) BigDecimal rotationDegrees,
        @RequestParam(required = false) Boolean flipHorizontal,
        @RequestParam(required = false) Boolean flipVertical,
        @RequestPart("file") MultipartFile file
    ) {
        return productBoxModelFaceService.uploadOrReplace(
            id,
            faceName,
            file,
            rotationDegrees,
            flipHorizontal,
            flipVertical
        );
    }

    @GetMapping("/{id}/faces/{faceName}/file")
    public ResponseEntity<Resource> getFaceFile(
        @PathVariable UUID id,
        @PathVariable String faceName
    ) {
        Resource resource = productBoxModelFaceService.getFile(id, faceName);
        return ResponseEntity.ok()
            .contentType(productBoxModelFaceService.getMediaType(id, faceName))
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

    @DeleteMapping("/{id}/faces/{faceName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFace(
        @PathVariable UUID id,
        @PathVariable String faceName
    ) {
        productBoxModelFaceService.delete(id, faceName);
    }
}
