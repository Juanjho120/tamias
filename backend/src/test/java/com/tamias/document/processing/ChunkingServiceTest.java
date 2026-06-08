package com.tamias.document.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkingServiceTest {

    @Test
    void shouldCreateSingleChunkWhenTextIsSmall() {
        ChunkingService service = new ChunkingService(4000, 600);

        List<TextChunk> chunks = service.split("This is a small document.");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().index()).isZero();
        assertThat(chunks.getFirst().content()).contains("small document");
        assertThat(chunks.getFirst().tokenCount()).isPositive();
    }

    @Test
    void shouldCreateMultipleChunksWhenTextIsLarge() {
        ChunkingService service = new ChunkingService(100, 20);

        String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(20);

        List<TextChunk> chunks = service.split(text);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks)
                .extracting(TextChunk::index)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, chunks.size()).boxed().toList());
    }
}
