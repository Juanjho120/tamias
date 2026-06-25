package com.tamias.image.dto;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.maintenance.enums.MaintenanceImageRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageUploadResponse(
        UUID id,
        UUID parentId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Boolean cover,
        MaintenanceImageRole imageRole,
        ImageStatus status,
        OffsetDateTime createdAt,
        String fileUrl,
        Integer fileUrlExpiresIn
) {
}
