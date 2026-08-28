package com.example.ragknowledgeservice.common;

import com.example.ragknowledgeservice.dto.UploadDocumentCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MultipartFileMapper {

    private MultipartFileMapper() {
    }

    public static UploadDocumentCommand toUploadDocumentCommand(MultipartFile file) {
        try {
            return new UploadDocumentCommand(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read uploaded file", exception);
        }
    }
}
