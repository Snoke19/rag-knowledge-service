package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.dto.FileContent;
import com.example.ragknowledgeservice.dto.SavedFile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DocumentService {

    public SavedFile saveDocument(FileContent fileContent) {
        return new SavedFile(UUID.randomUUID().toString(), DocumentStatus.UPLOADED);
    }
}
