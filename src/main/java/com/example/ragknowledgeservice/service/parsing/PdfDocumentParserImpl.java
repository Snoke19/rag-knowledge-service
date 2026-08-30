package com.example.ragknowledgeservice.service.parsing;

import com.example.ragknowledgeservice.exception.DocumentParsingException;
import com.example.ragknowledgeservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PdfDocumentParserImpl implements DocumentParser {

    private final DocumentService documentService;

    @Override
    public List<DocumentPage> parse(UUID documentId) {
        byte[] document = documentService.downloadDocument(documentId);

        try (PDDocument pdf = Loader.loadPDF(document)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            List<DocumentPage> pages = new ArrayList<>();

            for (int pageNumber = 1; pageNumber <= pdf.getNumberOfPages(); pageNumber++) {
                textStripper.setStartPage(pageNumber);
                textStripper.setEndPage(pageNumber);

                String text = textStripper.getText(pdf);
                pages.add(new DocumentPage(documentId, pageNumber, text, Map.of()));
            }

            return pages;
        } catch (Exception e) {
            throw new DocumentParsingException("Failed to parse document: " + documentId, e);
        }
    }
}
