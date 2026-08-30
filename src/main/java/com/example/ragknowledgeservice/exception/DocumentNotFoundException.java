package com.example.ragknowledgeservice.exception;

import lombok.Getter;

@Getter
public class DocumentNotFoundException extends RuntimeException {

    private final String documentId;

    public DocumentNotFoundException(String message, String documentId) {
        super(message);
        this.documentId = documentId;
    }
}
