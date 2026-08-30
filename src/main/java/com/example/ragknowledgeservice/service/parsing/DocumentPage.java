package com.example.ragknowledgeservice.service.parsing;

import java.util.UUID;

public record DocumentPage(
    UUID documentId,
    int pageNumber,
    String text,
    String sourceFilename,
    int extractionOrder
) {
}
