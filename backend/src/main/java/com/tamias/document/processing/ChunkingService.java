package com.tamias.document.processing;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;

    public ChunkingService(
            @Value("${tamias.ai.chunk-size:4000}") int chunkSize,
            @Value("${tamias.ai.chunk-overlap:600}") int chunkOverlap
    ) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<TextChunk> split(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String normalizedContent = normalizeWhitespace(content);

        if (normalizedContent.length() <= chunkSize) {
            return List.of(new TextChunk(0, normalizedContent, estimateTokenCount(normalizedContent)));
        }

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < normalizedContent.length()) {
            int end = Math.min(start + chunkSize, normalizedContent.length());
            int adjustedEnd = adjustEndToWordBoundary(normalizedContent, start, end);

            String chunkContent = normalizedContent.substring(start, adjustedEnd).trim();

            if (!chunkContent.isBlank()) {
                chunks.add(new TextChunk(index++, chunkContent, estimateTokenCount(chunkContent)));
            }

            if (adjustedEnd >= normalizedContent.length()) {
                break;
            }

            start = Math.max(0, adjustedEnd - chunkOverlap);
            start = adjustStartToWordBoundary(normalizedContent, start);
        }

        return chunks;
    }

    private String normalizeWhitespace(String content) {
        return content
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private int adjustEndToWordBoundary(String content, int start, int end) {
        if (end >= content.length()) {
            return content.length();
        }

        int candidate = end;

        while (candidate > start && !Character.isWhitespace(content.charAt(candidate - 1))) {
            candidate--;
        }

        return candidate > start ? candidate : end;
    }

    private int adjustStartToWordBoundary(String content, int start) {
        int candidate = start;

        while (candidate < content.length() && !Character.isWhitespace(content.charAt(candidate))) {
            candidate++;
        }

        return Math.min(candidate, content.length());
    }

    private int estimateTokenCount(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        return Math.max(1, content.length() / 4);
    }
}
