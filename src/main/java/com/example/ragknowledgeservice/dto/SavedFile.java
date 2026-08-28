package com.example.ragknowledgeservice.dto;

import com.example.ragknowledgeservice.common.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SavedFile {

    private String documentId;
    private DocumentStatus status;
}
