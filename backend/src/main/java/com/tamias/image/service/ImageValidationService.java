package com.tamias.image.service;

import com.tamias.common.exception.BadRequestException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final long maxImageSizeBytes;
    private final String maxImageSizeLabel;

    public ImageValidationService(
            @Value("${tamias.upload.max-image-size:${MAX_FILE_SIZE:25MB}}") String maxImageSize
    ) {
        DataSize parsedMaxImageSize = DataSize.parse(maxImageSize);
        this.maxImageSizeBytes = parsedMaxImageSize.toBytes();
        this.maxImageSizeLabel = maxImageSize;
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > maxImageSizeBytes) {
            throw new BadRequestException("Image exceeds maximum allowed size of " + maxImageSizeLabel);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only JPEG, PNG and WEBP images are allowed");
        }
    }
}