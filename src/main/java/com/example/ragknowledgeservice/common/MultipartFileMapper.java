package com.example.ragknowledgeservice.common;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public class MultipartFileMapper {

    private MultipartFileMapper() {
    }

    public static UploadDocumentCommand toUploadDocumentCommand(MultipartFile file) {
        try {
            String originalFilename = Optional
                .ofNullable(file.getOriginalFilename())
                .orElse("unknown")
                .replace('\\', '/');

            originalFilename = originalFilename.substring(originalFilename.lastIndexOf('/') + 1);

            return new UploadDocumentCommand(
                originalFilename,
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read uploaded file", exception);
        }
    }
}
