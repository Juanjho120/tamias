package com.tamias.document.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tamias.storage.s3")
public record S3StorageProperties(
        String bucket,
        String region,
        Integer presignedUrlExpirationSeconds
) {
    public int effectivePresignedUrlExpirationSeconds() {
        return presignedUrlExpirationSeconds != null ? presignedUrlExpirationSeconds : 300;
    }
}
