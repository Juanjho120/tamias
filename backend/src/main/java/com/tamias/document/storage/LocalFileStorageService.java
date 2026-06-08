package com.tamias.document.storage;

import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
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
    public StoredFile store(MultipartFile file, String organizationId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        try {
            Files.createDirectories(rootPath.resolve(organizationId));

            String safeOriginalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "document";

            String storageKey = organizationId + "/" + UUID.randomUUID() + "_" + safeOriginalName;
            Path targetPath = rootPath.resolve(storageKey).normalize();

            if (!targetPath.startsWith(rootPath)) {
                throw new BadRequestException("Invalid file path");
            }

            file.transferTo(targetPath);

            return new StoredFile(
                    storageKey,
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
    public String buildDownloadUrl(String storageKey, String documentId) {
        return publicBaseUrl + "/api/v1/documents/" + documentId + "/file";
    }

    @Override
    public int getDownloadUrlExpirationSeconds() {
        return expirationSeconds;
    }
}
