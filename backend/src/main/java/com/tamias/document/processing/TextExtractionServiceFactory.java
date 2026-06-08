package com.tamias.document.processing;

import com.tamias.common.exception.BadRequestException;
import com.tamias.document.entity.Document;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextExtractionServiceFactory {

    private final List<TextExtractionService> extractionServices;

    public TextExtractionServiceFactory(List<TextExtractionService> extractionServices) {
        this.extractionServices = extractionServices;
    }

    public TextExtractionService getExtractor(Document document) {
        return extractionServices.stream()
                .filter(extractionService -> extractionService.supports(document))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Text extraction is not supported for this document type yet"
                ));
    }
}
