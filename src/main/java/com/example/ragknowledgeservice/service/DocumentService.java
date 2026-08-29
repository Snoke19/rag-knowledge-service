package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.entities.Document;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileStorage fileStorage;
    private final DocumentRepository documentRepository;
    private final StorageTransactionManager storageTransactionManager;

    public SavedFile saveDocument(UploadDocumentCommand command) {
        String documentId = UUID.randomUUID().toString();
        String storageKey = "documents/" + documentId + "/source.pdf";

        return storageTransactionManager.execute(context -> {

            fileStorage.put(
                storageKey,
                command.content(),
                command.size(),
                command.contentType()
            );

            context.register(() -> fileStorage.delete(storageKey));

            Document document = Document.builder()
                .id(documentId)
                .title(command.filename())
                .contentType(command.contentType())
                .size(command.size())
                .storageKey(storageKey)
                .build();

            Document savedDocument = documentRepository.save(document);

            return new SavedFile(savedDocument.getId(), DocumentStatus.UPLOADED);
        });
    }
}
