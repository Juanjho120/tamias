package com.tamias.document.storage;

import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@ConditionalOnProperty(prefix = "tamias.storage", name = "provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public S3FileStorageService(
        S3Client s3Client,
        S3Presigner s3Presigner,
        S3StorageProperties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public StoredFile store(MultipartFile file, String storageFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        validateConfiguration();

        String contentType = file.getContentType() != null
            ? file.getContentType()
            : "application/octet-stream";
        String normalizedStorageFolder = normalizeStorageFolder(storageFolder);
        String storageKey = buildStorageKey(file, normalizedStorageFolder);
        String filepath = properties.bucket() + "/" + normalizedStorageFolder;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return new StoredFile(storageKey, filepath, contentType, file.getSize());
        } catch (IOException ex) {
            throw new BadRequestException("Could not read file for upload");
        } catch (S3Exception ex) {
            throw new BadRequestException("Could not store file in S3");
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        validateConfiguration();

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(storageKey)
                .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            byte[] bytes = responseBytes.asByteArray();

            return new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return extractFilename(storageKey);
                }
            };
        } catch (NoSuchKeyException ex) {
            throw new NotFoundException("File not found");
        } catch (S3Exception ex) {
            throw new NotFoundException("File not found");
        }
    }

    @Override
    public String buildDownloadUrl(String storageKey, String documentId) {
        return buildFileUrl(storageKey);
    }

    @Override
    public String buildFileUrl(String storageKey) {
        validateConfiguration();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(properties.bucket())
            .key(storageKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(getDownloadUrlExpirationSeconds()))
            .getObjectRequest(getObjectRequest)
            .build();

        return s3Presigner.presignGetObject(presignRequest)
            .url()
            .toString();
    }

    @Override
    public int getDownloadUrlExpirationSeconds() {
        return properties.effectivePresignedUrlExpirationSeconds();
    }

    private String buildStorageKey(MultipartFile file, String normalizedStorageFolder) {
        String safeOriginalName = file.getOriginalFilename() != null
            ? sanitizeFilename(file.getOriginalFilename())
            : "file";

        return normalizedStorageFolder + "/" + UUID.randomUUID() + "_" + safeOriginalName;
    }

    private String normalizeStorageFolder(String storageFolder) {
        if (storageFolder == null || storageFolder.isBlank()) {
            throw new BadRequestException("Invalid storage folder");
        }

        String normalized = storageFolder.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            throw new BadRequestException("Invalid storage folder");
        }

        StringBuilder builder = new StringBuilder();
        StringBuilder currentPart = new StringBuilder();
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '/') {
                appendSanitizedPathPart(builder, currentPart);
                currentPart.setLength(0);
            } else {
                currentPart.append(current);
            }
        }
        appendSanitizedPathPart(builder, currentPart);

        String result = builder.toString();
        if (result.isBlank()) {
            throw new BadRequestException("Invalid storage folder");
        }
        return result;
    }

    private void appendSanitizedPathPart(StringBuilder builder, StringBuilder rawPart) {
        if (rawPart == null || rawPart.length() == 0) {
            return;
        }

        String sanitized = sanitizePathPart(rawPart.toString());
        if (sanitized.isBlank()) {
            return;
        }

        if (builder.length() > 0) {
            builder.append('/');
        }
        builder.append(sanitized);
    }

    private String sanitizePathPart(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (isSafePathCharacter(current)) {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    private String sanitizeFilename(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (isSafePathCharacter(current)) {
                builder.append(current);
            } else {
                builder.append('_');
            }
        }

        String sanitized = builder.toString();
        return sanitized.isBlank() ? "file" : sanitized;
    }

    private boolean isSafePathCharacter(char value) {
        return (value >= 'a' && value <= 'z')
            || (value >= 'A' && value <= 'Z')
            || (value >= '0' && value <= '9')
            || value == '.'
            || value == '_'
            || value == '-';
    }

    private String extractFilename(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return "file";
        }

        int index = storageKey.lastIndexOf('/');
        if (index < 0 || index == storageKey.length() - 1) {
            return storageKey;
        }
        return storageKey.substring(index + 1);
    }

    private void validateConfiguration() {
        if (properties.bucket() == null || properties.bucket().isBlank()) {
            throw new BadRequestException("S3 bucket is not configured");
        }
        if (properties.region() == null || properties.region().isBlank()) {
            throw new BadRequestException("S3 region is not configured");
        }
    }
}
