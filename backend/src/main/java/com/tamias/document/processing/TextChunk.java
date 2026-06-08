package com.tamias.document.processing;

public record TextChunk(
        int index,
        String content,
        int tokenCount
) {
}
