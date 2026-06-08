package com.tamias.document.processing;

import com.tamias.document.entity.Document;
import org.springframework.core.io.Resource;

public interface TextExtractionService {

    boolean supports(Document document);

    ExtractedText extract(Document document, Resource resource);
}
