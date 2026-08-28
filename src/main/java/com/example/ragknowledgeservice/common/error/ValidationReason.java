package com.example.ragknowledgeservice.common.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ValidationReason {

    FILE_REQUIRED("File is required."),
    EMPTY_FILE("File must not be empty."),
    UNSUPPORTED_FILE_TYPE("Only PDF files are supported.");

    private final String message;
}