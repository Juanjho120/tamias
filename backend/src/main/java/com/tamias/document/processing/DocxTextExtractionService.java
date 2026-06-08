package com.tamias.document.processing;

import com.tamias.common.exception.BadRequestException;
import com.tamias.document.entity.Document;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class DocxTextExtractionService implements TextExtractionService {

    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Override
    public boolean supports(Document document) {
        return CONTENT_TYPE.equalsIgnoreCase(document.getContentType())
                || document.getOriginalFilename().toLowerCase().endsWith(".docx");
    }

    @Override
    public ExtractedText extract(Document document, Resource resource) {
        try (InputStream inputStream = resource.getInputStream();
             XWPFDocument xwpfDocument = new XWPFDocument(inputStream)) {

            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : xwpfDocument.getParagraphs()) {
                String text = paragraph.getText();

                if (text != null && !text.isBlank()) {
                    content.append(text).append(System.lineSeparator());
                }
            }

            xwpfDocument.getTables().forEach(table ->
                    table.getRows().forEach(row ->
                            row.getTableCells().forEach(cell -> {
                                String text = cell.getText();

                                if (text != null && !text.isBlank()) {
                                    content.append(text).append(System.lineSeparator());
                                }
                            })
                    )
            );

            return new ExtractedText(
                    content.toString(),
                    1,
                    Map.of("extractor", "apache-poi")
            );
        } catch (IOException exception) {
            throw new BadRequestException("Could not extract text from DOCX document");
        }
    }
}
