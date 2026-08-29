package com.example.ragknowledgeservice.dto;

import com.example.ragknowledgeservice.common.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SavedFile {

    private UUID documentId;
    private DocumentStatus status;
}
