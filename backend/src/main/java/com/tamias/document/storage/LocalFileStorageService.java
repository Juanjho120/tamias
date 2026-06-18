package com.tamias.document.storage;

import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "tamias.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootPath;
    private final String publicBaseUrl;
    private final int expirationSeconds;

    public LocalFileStorageService(
            @Value("${tamias.storage.local-root:uploads/documents}") String localRoot,
            @Value("${tamias.storage.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${tamias.storage.download-url-expiration-seconds:300}") int expirationSeconds
    ) {
        this.rootPath = Path.of(localRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public StoredFile store(MultipartFile file, String storageFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        try {
            String normalizedStorageFolder = normalizeStorageFolder(storageFolder);
            Path folderPath = rootPath.resolve(normalizedStorageFolder).normalize();
            if (!folderPath.startsWith(rootPath)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.createDirectories(folderPath);

            String safeOriginalName = file.getOriginalFilename() != null
                    ? sanitizeFilename(file.getOriginalFilename())
                    : "document";
            String storageKey = normalizedStorageFolder + "/" + UUID.randomUUID() + "_" + safeOriginalName;
            Path targetPath = rootPath.resolve(storageKey).normalize();
            if (!targetPath.startsWith(rootPath)) {
                throw new BadRequestException("Invalid file path");
            }

            file.transferTo(targetPath);
            return new StoredFile(
                    storageKey,
                    folderPath.toString(),
                    file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                    file.getSize()
            );
        } catch (IOException ex) {
            throw new BadRequestException("Could not store file");
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            Path filePath = rootPath.resolve(storageKey).normalize();
            if (!filePath.startsWith(rootPath) || !Files.exists(filePath)) {
                throw new NotFoundException("File not found");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("File not found");
            }
            return resource;
        } catch (IOException ex) {
            throw new NotFoundException("File not found");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new BadRequestException("Invalid storage key");
        }

        try {
            Path filePath = rootPath.resolve(storageKey).normalize();
            if (!filePath.startsWith(rootPath)) {
                throw new BadRequestException("Invalid file path");
            }
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new BadRequestException("Could not delete file");
        }
    }

    @Override
    public String buildDownloadUrl(String storageKey, String documentId) {
        return publicBaseUrl + "/api/v1/documents/" + documentId + "/file";
    }

    @Override
    public String buildFileUrl(String storageKey) {
        return null;
    }

    @Override
    public int getDownloadUrlExpirationSeconds() {
        return expirationSeconds;
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
}
