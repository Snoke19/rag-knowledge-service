package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.common.hasher.ContentHasher;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.exception.DocumentNotFoundException;
import com.example.ragknowledgeservice.exception.StorageException;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final FileStorage fileStorage;
    private final ContentHasher contentHasher;
    private final DocumentRepository documentRepository;
    private final StorageTransactionManager storageTransactionManager;

    public SavedFile saveDocument(UploadDocumentCommand command) {
        UUID documentId = UUID.randomUUID();
        String storageKey = "documents/" + documentId + "/source.pdf";

        DocumentUploadContent content = prepareContent(command);

        return storageTransactionManager.execute(context -> {

            fileStorage.put(
                storageKey,
                content.inputStream(),
                content.size(),
                command.contentType()
            );

            context.register(() -> fileStorage.delete(storageKey));

            DocumentMetadata documentMetadata = DocumentMetadata.builder()
                .documentId(documentId)
                .title(command.filename())
                .contentType(command.contentType())
                .size(content.size())
                .storageKey(storageKey)
                .status(DocumentStatus.UPLOADED)
                .contentSha256(content.sha256())
                .build();

            DocumentMetadata savedDocumentMetadata = documentRepository.save(documentMetadata);

            return new SavedFile(savedDocumentMetadata.getDocumentId(), savedDocumentMetadata.getStatus());
        });
    }

    @Transactional(readOnly = true)
    public DocumentMetadata getDocumentMetadata(UUID documentId) {
        return documentRepository.findByDocumentId(documentId).orElseThrow(() ->
            new DocumentNotFoundException("Metadata of the document not found!", documentId.toString())
        );
    }

    @Transactional(readOnly = true)
    public byte[] downloadDocument(UUID documentId) {
        DocumentMetadata document = documentRepository.findByDocumentId(documentId).orElseThrow(() ->
            new DocumentNotFoundException("Metadata of the document not found!", documentId.toString())
        );

        try (InputStream inputStream = fileStorage.get(document.getStorageKey())) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to read document", e);
        }
    }

    public void deleteAll() {
        fileStorage.deleteAll();
    }

    private DocumentUploadContent prepareContent(UploadDocumentCommand command) {
        try (InputStream inputStream = command.content()) {
            byte[] content = inputStream.readAllBytes();

            return new DocumentUploadContent(content, contentHasher.sha256(content));
        } catch (IOException e) {
            throw new StorageException("Failed to read document content", e);
        }
    }

    private record DocumentUploadContent(byte[] content, String sha256) {
        InputStream inputStream() {
            return new ByteArrayInputStream(content);
        }

        long size() {
            return content.length;
        }
    }
}
