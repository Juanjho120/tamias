package com.tamias.document.processing;

import com.tamias.common.exception.BadRequestException;
import com.tamias.document.entity.Document;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PlainTextExtractionService implements TextExtractionService {

    private static final String CONTENT_TYPE = "text/plain";

    @Override
    public boolean supports(Document document) {
        return CONTENT_TYPE.equalsIgnoreCase(document.getContentType())
                || document.getOriginalFilename().toLowerCase().endsWith(".txt");
    }

    @Override
    public ExtractedText extract(Document document, Resource resource) {
        try {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);

            return new ExtractedText(
                    content,
                    1,
                    Map.of("extractor", "plain-text")
            );
        } catch (IOException exception) {
            throw new BadRequestException("Could not extract text from TXT document");
        }
    }
}
