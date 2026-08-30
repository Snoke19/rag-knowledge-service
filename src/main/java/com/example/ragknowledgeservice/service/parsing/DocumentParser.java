package com.example.ragknowledgeservice.service.parsing;

import java.util.List;
import java.util.UUID;

public interface DocumentParser {

    List<DocumentPage> parse(UUID documentId);
}
