package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.dto.UploadDocumentCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    public SavedFile saveDocument(UploadDocumentCommand uploadDocumentCommand) {
        log.debug("Saving document: {}", uploadDocumentCommand.filename());
        return new SavedFile(UUID.randomUUID().toString(), DocumentStatus.UPLOADED);
    }
}
