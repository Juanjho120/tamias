package com.tamias.document.processing;

import java.util.Map;

public record ExtractedText(
        String content,
        int pageCount,
        Map<String, Object> metadata
) {
}
