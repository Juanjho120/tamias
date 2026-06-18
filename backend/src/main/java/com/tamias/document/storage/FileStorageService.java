package com.tamias.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFile store(MultipartFile file, String storageFolder);

    Resource loadAsResource(String storageKey);

    void delete(String storageKey);

    String buildDownloadUrl(String storageKey, String documentId);

    String buildFileUrl(String storageKey);

    int getDownloadUrlExpirationSeconds();
}
