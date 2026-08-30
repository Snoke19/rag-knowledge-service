package com.example.ragknowledgeservice.service.parsing;

import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.exception.DocumentParsingException;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfDocumentParserImplTest {

    @Mock
    private DocumentService documentService;

    @Test
    void parsesPdfIntoOrderedDocumentPages() throws Exception {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createPdf(
            "First page text",
            "Second page text"
        );

        DocumentMetadata metadata = createDocumentMetadata(documentId, "sample.pdf");

        when(documentService.downloadDocument(documentId)).thenReturn(pdf);
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);
        List<DocumentPage> pages = parser.parse(documentId);

        assertThat(pages).hasSize(2);

        assertThat(pages.getFirst().documentId()).isEqualTo(documentId);
        assertThat(pages.getFirst().pageNumber()).isEqualTo(1);
        assertThat(pages.getFirst().text()).contains("First page text");
        assertThat(pages.get(0).sourceFilename()).isEqualTo("sample.pdf");
        assertThat(pages.get(0).extractionOrder()).isEqualTo(1);

        assertThat(pages.get(1).documentId()).isEqualTo(documentId);
        assertThat(pages.get(1).pageNumber()).isEqualTo(2);
        assertThat(pages.get(1).text()).contains("Second page text");
        assertThat(pages.get(1).sourceFilename()).isEqualTo("sample.pdf");
        assertThat(pages.get(1).extractionOrder()).isEqualTo(2);
    }

    @Test
    void throwsDocumentParsingExceptionWhenPdfIsInvalid() {
        UUID documentId = UUID.randomUUID();
        DocumentMetadata metadata = createDocumentMetadata(documentId, "invalid.pdf");

        when(documentService.downloadDocument(documentId)).thenReturn("not a pdf".getBytes(StandardCharsets.UTF_8));
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);

        assertThatThrownBy(() -> parser.parse(documentId))
            .isInstanceOf(DocumentParsingException.class)
            .hasMessageContaining(documentId.toString());
    }

    @Test
    void returnsEmptyTextForPageWithoutExtractableText() throws Exception {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createPdfWithEmptyPage();

        DocumentMetadata metadata = createDocumentMetadata(documentId, "empty-page.pdf");

        when(documentService.downloadDocument(documentId)).thenReturn(pdf);
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);

        List<DocumentPage> pages = parser.parse(documentId);

        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst().documentId()).isEqualTo(documentId);
        assertThat(pages.getFirst().pageNumber()).isEqualTo(1);
        assertThat(pages.getFirst().text()).isNotNull();
        assertThat(pages.getFirst().sourceFilename()).isEqualTo("empty-page.pdf");
        assertThat(pages.getFirst().extractionOrder()).isEqualTo(1);
    }

    @Test
    void loadsPdfUsingDocumentId() throws Exception {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createPdf("Test page");

        DocumentMetadata metadata = createDocumentMetadata(documentId, "sample.pdf");

        when(documentService.downloadDocument(documentId)).thenReturn(pdf);
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);
        parser.parse(documentId);

        verify(documentService).downloadDocument(documentId);
        verify(documentService).getDocumentMetadata(documentId);
        verifyNoMoreInteractions(documentService);
    }

    @Test
    void wrapsPdfBoxFailureInDocumentParsingException() {
        UUID documentId = UUID.randomUUID();

        DocumentMetadata metadata = createDocumentMetadata(
            documentId,
            "invalid-pdf.pdf"
        );

        when(documentService.downloadDocument(documentId)).thenReturn("invalid-pdf".getBytes(StandardCharsets.UTF_8));
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);

        DocumentParser parser = new PdfDocumentParserImpl(documentService);

        assertThatThrownBy(() -> parser.parse(documentId))
            .isInstanceOf(DocumentParsingException.class)
            .hasMessage("Failed to parse document: " + documentId)
            .hasCauseInstanceOf(Exception.class);
    }

    private DocumentMetadata createDocumentMetadata(UUID documentId, String filename) {
        return DocumentMetadata.builder()
            .documentId(documentId)
            .title(filename)
            .build();
    }

    private byte[] createPdfWithEmptyPage() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            document.addPage(new PDPage());

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createPdf(String... pageTexts) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (String pageText : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {

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