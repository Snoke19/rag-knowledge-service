package com.example.ragknowledgeservice.command;

import java.io.InputStream;

public record UploadDocumentCommand(
    String filename,
    String contentType,
    long size,
    InputStream content
) {
}
