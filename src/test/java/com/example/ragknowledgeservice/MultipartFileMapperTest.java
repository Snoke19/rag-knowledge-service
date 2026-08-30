package com.example.ragknowledgeservice;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.MultipartFileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultipartFileMapperTest {

    @Test
    void stripsWindowsPathFromOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "C:\\temp\\company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test".getBytes()
        );

        UploadDocumentCommand command = MultipartFileMapper.toUploadDocumentCommand(file);

        assertEquals("company-handbook.pdf", command.filename());
    }

    @Test
    void stripsPathFromOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "../../company-handbook.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "%PDF-1.4 test".getBytes()
        );

        UploadDocumentCommand command = MultipartFileMapper.toUploadDocumentCommand(file);

        assertEquals("company-handbook.pdf", command.filename());
    }
}
