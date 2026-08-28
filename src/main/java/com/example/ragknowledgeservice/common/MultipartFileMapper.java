package com.example.ragknowledgeservice.common;

import com.example.ragknowledgeservice.dto.FileContent;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class MultipartFileMapper {

    private MultipartFileMapper() {
    }

    public static FileContent toFileContent(MultipartFile file) {
        try {
            return new FileContent(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read uploaded file", exception);
        }
    }
}
