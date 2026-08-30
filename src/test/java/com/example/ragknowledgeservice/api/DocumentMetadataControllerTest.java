package com.example.ragknowledgeservice.api;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.common.MultipartFileMapper;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.exception.GlobalExceptionHandler;
import com.example.ragknowledgeservice.exception.StorageException;
import com.example.ragknowledgeservice.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartException;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
public class DocumentMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void uploadsDocumentAndReturnsCreatedResponse() throws Exception {
        UUID documentId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test content".getBytes()
        );

        SavedFile savedFile = new SavedFile(documentId, DocumentStatus.UPLOADED);

        when(documentService.saveDocument(any())).thenReturn(savedFile);

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/documents/" + documentId))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.documentId").value(documentId.toString()))
            .andExpect(jsonPath("$.status").value("UPLOADED"));

        verify(documentService).saveDocument(any());
    }

    @Test
    void rejectsRequestWithoutFile() throws Exception {
        mockMvc.perform(multipart("/api/documents"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Missing multipart part"))
            .andExpect(jsonPath("$.detail").value("Required multipart part 'file' is missing."))
            .andExpect(jsonPath("$.type").value("https://ragknowledgeservice.example.com/problems/missing-request-part"));
    }

    @Test
    void rejectsNonPdfFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "company-handbook.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "not a pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.detail").value("One or more request fields are invalid."))
            .andExpect(jsonPath("$.type").value("https://ragknowledgeservice.example.com/problems/validation-error"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("file"))
            .andExpect(jsonPath("$.errors[0].reason").value("UNSUPPORTED_FILE_TYPE"))
            .andExpect(jsonPath("$.errors[0].detail")
                .value("Only PDF files are supported."));
    }

    @Test
    void acceptsMultipartFormData() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test content".getBytes()
        );

        when(documentService.saveDocument(any())).thenReturn(new SavedFile(
            UUID.randomUUID(),
            DocumentStatus.UPLOADED
        ));

        mockMvc.perform(multipart("/api/documents")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated());
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0].reason").value("EMPTY_FILE"))
            .andExpect(jsonPath("$.errors[0].detail").value("File must not be empty."));
    }

    @Test
    void rejectsFileWithInvalidPdfContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "fake.pdf", MediaType.APPLICATION_PDF_VALUE, "not a real pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.errors[0].reason").value("INVALID_PDF_CONTENT"))
            .andExpect(jsonPath("$.errors[0].detail").value("File content is not a valid PDF."));
    }

    @Test
    void rejectsMalformedMultipartRequest() throws Exception {
        MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new BrokenMultipartController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        mvc.perform(post("/api/documents"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Invalid multipart request"))
            .andExpect(jsonPath("$.type").value("https://ragknowledgeservice.example.com/problems/invalid-multipart"));
    }

    @Test
    void rejectsFileWithPdfContentTypeButInvalidContent() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "not a real pdf".getBytes()
        );

        mockMvc.perform(multipart("/api/documents").file(file))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].reason")
                .value("INVALID_PDF_CONTENT"));
    }

    @Test
    void stripsPathFromOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../../company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test".getBytes()
        );

        UploadDocumentCommand command =
            MultipartFileMapper.toUploadDocumentCommand(file);

        assertThat(command.filename())
            .isEqualTo("company-handbook.pdf");
    }

    @Test
    void stripsWindowsPathFromOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "C:\\temp\\company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test".getBytes()
        );

        UploadDocumentCommand command =
            MultipartFileMapper.toUploadDocumentCommand(file);

        assertThat(command.filename())
            .isEqualTo("company-handbook.pdf");
    }

    @Test
    void returnsInternalServerErrorWhenStorageFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test content".getBytes()
        );

        when(documentService.saveDocument(any())).thenThrow(new StorageException("Storage transaction failed"));

        mockMvc.perform(
                multipart("/api/documents")
                    .file(file)
            )
            .andExpect(status().isInternalServerError())
            .andExpect(
                content().contentTypeCompatibleWith("application/problem+json")
            )
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.title")
                .value("Storage operation failed"))
            .andExpect(jsonPath("$.detail")
                .value("The document could not be stored."))
            .andExpect(jsonPath("$.type")
                .value(
                    "https://ragknowledgeservice.example.com/problems/storage-error"
                ));
    }

    @RestController
    static class BrokenMultipartController {

        @PostMapping("/api/documents")
        public void handle() {
            throw new MultipartException("Failed to parse multipart servlet request");
        }
    }
}
