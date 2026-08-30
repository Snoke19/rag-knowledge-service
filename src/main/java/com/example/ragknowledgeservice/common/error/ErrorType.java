package com.example.ragknowledgeservice.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorType {

    INVALID_MULTIPART("https://ragknowledgeservice.example.com/problems/invalid-multipart"),
    MISSING_REQUEST_PART("https://ragknowledgeservice.example.com/problems/missing-request-part"),
    STORAGE_ERROR("https://ragknowledgeservice.example.com/problems/storage-error"),
    INTERNAL_ERROR("https://ragknowledgeservice.example.com/problems/internal-error"),
    DOCUMENT_NOT_FOUND("https://ragknowledgeservice.example.com/problems/document-not-found"),
    VALIDATION_ERROR("https://ragknowledgeservice.example.com/problems/validation-error");

    private final String uri;
}