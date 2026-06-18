package com.tamias.document.storage;

public record StoredFile(
    String storageKey,
    String filepath,
    String contentType,
    Long sizeBytes
) {
}
