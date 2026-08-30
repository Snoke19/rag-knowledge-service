package com.example.ragknowledgeservice.service.parsing;

import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.service.DocumentService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
        givenDocument(documentId, pdf, "normal-text.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(2);
        assertThat(pages.get(0).text()).contains("First page");
        assertThat(pages.get(1).text()).contains("Second page");
    }

    @Test
    void doesNotExtractTextFromImageOnlyPage() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createImageOnlyPdf();

        givenDocument(documentId, pdf, "image-only.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst().text()).isBlank();
    }

    @Test
    void extractsHeadersAndFootersAsOrdinaryText() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createHeaderFooterPdf();
        givenDocument(documentId, pdf, "header-footer.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(2);

        assertThat(pages.get(0).text()).contains("Company Handbook").contains("Page 1");
        assertThat(pages.get(1).text()).contains("Company Handbook").contains("Page 2");
    }

    @Test
    void extractsTableContentButDoesNotPreserveTableStructure() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createTablePdf();
        givenDocument(documentId, pdf, "table.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(1);
        String text = pages.getFirst().text();
        assertThat(text)
            .contains("Name")
            .contains("Age")
            .contains("Alex")
            .contains("30")
            .contains("John")
            .contains("25");
    }

    @Test
    void extractsTextFromMultipleColumnsWithoutGuaranteeingHumanReadingOrder() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createTwoColumnPdf();
        givenDocument(documentId, pdf, "multiple-columns.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(1);
        String text = pages.getFirst().text();
        assertThat(text)
            .contains("Left column")
            .contains("Right column");
    }

    @Test
    void doesNotExtractTextEmbeddedInsideImage() throws IOException {
        UUID documentId = UUID.randomUUID();
        byte[] pdf = createPdfWithTextInsideImage();
        givenDocument(documentId, pdf, "text-in-image.pdf");

        List<DocumentPage> pages = parse(documentId);

        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst().text()).doesNotContain("Hidden image text");
    }

    private List<DocumentPage> parse(UUID documentId) {
        DocumentParser parser = new PdfDocumentParserImpl(documentService);
        return parser.parse(documentId);
    }

    private void givenDocument(UUID documentId, byte[] pdf, String filename) {
        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentId(documentId)
            .title(filename)
            .build();

        when(documentService.downloadDocument(documentId)).thenReturn(pdf);
        when(documentService.getDocumentMetadata(documentId)).thenReturn(metadata);
    }

    private byte[] createPdf(String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
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

    private byte[] createImageOnlyPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);
            BufferedImage image = new BufferedImage(500, 300, BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 500, 300);
            graphics.setColor(Color.BLACK);
            graphics.drawString("Scanned document page", 50, 100);
            graphics.dispose();

            ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();
            ImageIO.write(image, "png", imageOutput);

            PDImageXObject imageObject = PDImageXObject.createFromByteArray(document, imageOutput.toByteArray(), "scan");

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(imageObject, 50, 400, 500, 300);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createHeaderFooterPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            for (int pageNumber = 1; pageNumber <= 2; pageNumber++) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 750);
                    content.showText("Company Handbook");
                    content.endText();

                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText("Main page content");
                    content.endText();

                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 50);
                    content.showText("Page " + pageNumber);
                    content.endText();
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createTablePdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writeText(content, font, "Name", 50, 700);
                writeText(content, font, "Age", 200, 700);

                writeText(content, font, "Alex", 50, 670);
                writeText(content, font, "30", 200, 670);

                writeText(content, font, "John", 50, 640);
                writeText(content, font, "25", 200, 640);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createTwoColumnPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                writeText(content, font, "Left column text", 50, 700);
                writeText(content, font, "Right column text", 350, 700);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] createPdfWithTextInsideImage() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);
            BufferedImage image = new BufferedImage(500, 300, BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 500, 300);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Arial", Font.PLAIN, 24));
            graphics.drawString("Hidden image text", 50, 100);
            graphics.dispose();

            ByteArrayOutputStream imageOutput = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imageOutput);

            PDImageXObject imageObject = PDImageXObject.createFromByteArray(document, imageOutput.toByteArray(), "text-image");

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.drawImage(imageObject, 50, 400, 500, 300);
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private void writeText(PDPageContentStream content, PDType1Font font, String text, float x, float y) throws IOException {
        content.beginText();
        content.setFont(font, 12);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }
}