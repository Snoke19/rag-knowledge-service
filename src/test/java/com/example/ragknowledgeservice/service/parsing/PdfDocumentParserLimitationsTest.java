package com.example.ragknowledgeservice.service.parsing;

import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.service.DocumentService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfDocumentParserLimitationsTest {

    @Mock
    private DocumentService documentService;

    @Test
    void extractsNormalTextFromPdf() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createPdf("First page", "Second page");
        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentId(documentId)
            .title("normal-text.pdf")
            .build();

        when(documentService.downloadDocument(documentId)).thenReturn(pdf);
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);
        List<DocumentPage> pages = parser.parse(documentId);

        assertThat(pages).hasSize(2);
        assertThat(pages).allSatisfy(page -> {
            assertThat(page.documentId()).isEqualTo(documentId);
            assertThat(page.sourceFilename()).isEqualTo("normal-text.pdf");
        });

        assertThat(pages.get(0).pageNumber()).isEqualTo(1);
        assertThat(pages.get(0).extractionOrder()).isEqualTo(1);
        assertThat(pages.get(0).text()).contains("First page");

        assertThat(pages.get(1).pageNumber()).isEqualTo(2);
        assertThat(pages.get(1).extractionOrder()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("Second page");
    }

    private byte[] createPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType1Font font =
                new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(pageText);
                    content.endText();
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}