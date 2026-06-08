package com.tamias.document.dto;

public record DocumentDownloadUrlResponse(
        String url,
        Integer expiresIn
) {
}
