package com.example.ragknowledgeservice.service.parsing;

import java.util.Map;
import java.util.UUID;

public record DocumentPage(
    UUID documentId,
    int pageNumber,
    String text,
    Map<String, Object> metadata
) {
}
