package com.example.ragknowledgeservice.dto;

public record FileContent(
    String filename,
    String contentType,
    long size,
    byte[] content
) {
}
