package com.example.ragknowledgeservice.dto;

import java.io.InputStream;

public record UploadDocumentCommand(
    String filename,
    String contentType,
    long size,
    InputStream content
) {
}
