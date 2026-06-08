package com.tamias.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String organizationId);

    Resource loadAsResource(String storageKey);

    String buildDownloadUrl(String storageKey, String documentId);

    int getDownloadUrlExpirationSeconds();
}
