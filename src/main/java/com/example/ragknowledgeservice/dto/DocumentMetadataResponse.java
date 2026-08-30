package com.example.ragknowledgeservice.dto;

import com.example.ragknowledgeservice.common.DocumentStatus;

import java.util.UUID;

public record DocumentMetadataResponse(
    UUID documentId,
    String title,
    String contentType,
    long size,
    DocumentStatus status,
    String contentSha256
) {
}
