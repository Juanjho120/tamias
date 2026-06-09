package com.tamias.image.service;

import com.tamias.common.exception.BadRequestException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

    private static final long MAX_IMAGE_SIZE_BYTES = 8L * 1024L * 1024L;

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image exceeds maximum allowed size");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG and WEBP images are allowed");
        }
    }
}
