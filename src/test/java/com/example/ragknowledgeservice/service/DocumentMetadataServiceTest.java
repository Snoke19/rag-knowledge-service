package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.common.hasher.ContentHasher;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.exception.StorageException;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private FileStorage fileStorage;

    @Mock
    private ContentHasher contentHasher;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private StorageTransactionManager storageTransactionManager;

    private DocumentService documentService;

    @Test
    void shouldDelegateUploadToStorageTransactionManager() {
        documentService = new DocumentService(
            fileStorage,
            contentHasher,
            documentRepository,
            storageTransactionManager
        );

        UploadDocumentCommand command = new UploadDocumentCommand(
            "company-handbook.pdf",
            "application/pdf",
            1024L,
            new ByteArrayInputStream("%PDF-test".getBytes())
        );

        SavedFile expected = new SavedFile(UUID.randomUUID(), DocumentStatus.UPLOADED);

        when(storageTransactionManager.execute(any())).thenReturn(expected);

        SavedFile result = documentService.saveDocument(command);
        assertThat(result).isSameAs(expected);
        verify(storageTransactionManager).execute(any());
        verifyNoInteractions(fileStorage, documentRepository);
    }

    @Test
    void shouldPropagateStorageTransactionFailure() {
        documentService = new DocumentService(
            fileStorage,
            contentHasher,
            documentRepository,
            storageTransactionManager
        );

        UploadDocumentCommand command = new UploadDocumentCommand(
            "company-handbook.pdf",
            "application/pdf",
            1024L,
            new ByteArrayInputStream("%PDF-test".getBytes())
        );

        StorageException exception = new StorageException("Storage transaction failed");

        when(storageTransactionManager.execute(any())).thenThrow(exception);

        assertThatThrownBy(() -> documentService.saveDocument(command)).isSameAs(exception);
        verify(storageTransactionManager).execute(any());
        verifyNoInteractions(fileStorage, documentRepository);
    }
}