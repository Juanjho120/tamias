package com.tamias.image.payment.controller;

import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.payment.service.PaymentImageService;
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
@RequestMapping("/api/v1/payments/{paymentId}/images")
public class PaymentImageController {

    private final PaymentImageService paymentImageService;

    public PaymentImageController(PaymentImageService paymentImageService) {
        this.paymentImageService = paymentImageService;
    }

    @GetMapping
    public List<ImageResponse> findAll(@PathVariable UUID paymentId) {
        return paymentImageService.findAll(paymentId);
    }

    @GetMapping("/{imageId}")
    public ImageResponse findById(
            @PathVariable UUID paymentId,
            @PathVariable UUID imageId
    ) {
        return paymentImageService.findById(paymentId, imageId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ImageUploadResponse upload(
            @PathVariable UUID paymentId,
            @RequestPart("file") MultipartFile file
    ) {
        return paymentImageService.upload(paymentId, file);
    }

    @GetMapping("/{imageId}/file")
    public ResponseEntity<Resource> getFile(
            @PathVariable UUID paymentId,
            @PathVariable UUID imageId
    ) {
        Resource resource = paymentImageService.getFile(paymentId, imageId);

        return ResponseEntity.ok()
                .contentType(paymentImageService.getMediaType(paymentId, imageId))
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
            @PathVariable UUID paymentId,
            @PathVariable UUID imageId
    ) {
        paymentImageService.delete(paymentId, imageId);
    }
}
