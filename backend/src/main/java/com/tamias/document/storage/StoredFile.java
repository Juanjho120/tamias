package com.tamias.document.storage;

public record StoredFile(
        String storageKey,
        String contentType,
        Long sizeBytes
) {
}
