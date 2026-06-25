package com.tamias.image.dto;

import com.tamias.image.enums.ImageStatus;
import com.tamias.image.maintenance.enums.MaintenanceImageRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        UUID parentId,
        String originalFilename,
        String s3Key,
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
