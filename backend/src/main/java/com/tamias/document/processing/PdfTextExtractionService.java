package com.tamias.document.processing;

import com.tamias.common.exception.BadRequestException;
import com.tamias.document.entity.Document;
import java.io.IOException;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class PdfTextExtractionService implements TextExtractionService {

    private static final String CONTENT_TYPE = "application/pdf";

    @Override
    public boolean supports(Document document) {
        return CONTENT_TYPE.equalsIgnoreCase(document.getContentType())
                || document.getOriginalFilename().toLowerCase().endsWith(".pdf");
    }

    @Override
    public ExtractedText extract(Document document, Resource resource) {
        try (PDDocument pdfDocument = Loader.loadPDF(resource.getContentAsByteArray())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(pdfDocument);

            return new ExtractedText(
                    content,
                    pdfDocument.getNumberOfPages(),
                    Map.of("extractor", "pdfbox")
            );
        } catch (IOException exception) {
            throw new BadRequestException("Could not extract text from PDF document");
        }
    }
}
