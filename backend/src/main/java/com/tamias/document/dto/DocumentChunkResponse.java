package com.tamias.document.dto;

import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        Integer chunkIndex,
        String content,
        Integer tokenCount,
        String vectorStoreCollection,
        String vectorStoreId
) {
}
