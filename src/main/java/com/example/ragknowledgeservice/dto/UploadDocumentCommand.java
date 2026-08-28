package com.example.ragknowledgeservice.dto;

public record UploadDocumentCommand(
    String filename,
    String contentType,
    long size,
    byte[] content
) {
}
