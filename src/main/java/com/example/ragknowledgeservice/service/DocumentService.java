package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.StorageException;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileStorage fileStorage;
    private final DocumentRepository documentRepository;
    private final StorageTransactionManager storageTransactionManager;

    public SavedFile saveDocument(UploadDocumentCommand command) {
        UUID documentId = UUID.randomUUID();
        String storageKey = "documents/" + documentId + "/source.pdf";

        return storageTransactionManager.execute(context -> {

            fileStorage.put(
                storageKey,
                command.content(),
                command.size(),
                command.contentType()
            );

            context.register(() -> fileStorage.delete(storageKey));

            DocumentMetadata documentMetadata = DocumentMetadata.builder()
                .documentId(documentId)
                .title(command.filename())
                .contentType(command.contentType())
                .size(command.size())
                .storageKey(storageKey)
                .status(DocumentStatus.UPLOADED)
                .build();

            DocumentMetadata savedDocumentMetadata = documentRepository.save(documentMetadata);

            return new SavedFile(savedDocumentMetadata.getDocumentId(), savedDocumentMetadata.getStatus());
        });
    }

    @Transactional(readOnly = true)
    public DocumentMetadata getMetaDataDocument(UUID documentId) {
        return documentRepository.findByDocumentId(documentId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocument(UUID documentId) {
        DocumentMetadata document = documentRepository.findByDocumentId(documentId);

        try (InputStream inputStream = fileStorage.get(document.getStorageKey())) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read document", e);
        }
    }

    public void deleteAll() {
        fileStorage.deleteAll();
    }
}
