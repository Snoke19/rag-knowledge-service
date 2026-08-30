package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.common.hasher.ContentHasher;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.exception.DocumentNotFoundException;
import com.example.ragknowledgeservice.exception.StorageException;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.CompensationContext;
import com.example.ragknowledgeservice.service.storage.StorageOperation;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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

    @Test
    void savesDocumentSuccessfully() {
        byte[] content = pdfContent();
        String sha256 = "abc123";
        UUID documentId = UUID.randomUUID();
        UploadDocumentCommand command = command(content);
        DocumentMetadata savedMetadata = DocumentMetadata.builder()
            .documentId(documentId)
            .title("company-handbook.pdf")
            .contentType("application/pdf")
            .size(content.length)
            .storageKey("documents/" + documentId + "/source.pdf")
            .status(DocumentStatus.UPLOADED)
            .contentSha256(sha256)
            .build();

        when(contentHasher.sha256(content)).thenReturn(sha256);
        when(documentRepository.save(any(DocumentMetadata.class))).thenReturn(savedMetadata);
        when(storageTransactionManager.execute(any())).thenAnswer(invocation -> {
            StorageOperation<SavedFile> operation = invocation.getArgument(0);
            return operation.execute(new CompensationContext());
        });

        DocumentService documentService = createService();
        SavedFile result = documentService.saveDocument(command);

        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        verify(contentHasher).sha256(content);
        verify(fileStorage).put(anyString(), any(InputStream.class), eq((long) content.length), eq("application/pdf"));

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());

        DocumentMetadata metadata = captor.getValue();
        assertThat(metadata.getDocumentId()).isNotNull();
        assertThat(metadata.getTitle()).isEqualTo("company-handbook.pdf");
        assertThat(metadata.getContentType()).isEqualTo("application/pdf");
        assertThat(metadata.getSize()).isEqualTo(content.length);
        assertThat(metadata.getStorageKey()).startsWith("documents/").endsWith("/source.pdf");
        assertThat(metadata.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(metadata.getContentSha256()).isEqualTo(sha256);
    }

    @Test
    void calculatesSha256FromActualDocumentBytes() {
        byte[] content = pdfContent();
        String expectedSha256 = "expected-sha256";

        when(contentHasher.sha256(content)).thenReturn(expectedSha256);
        when(storageTransactionManager.execute(any())).thenAnswer(invocation -> {
            StorageOperation<SavedFile> operation = invocation.getArgument(0);
            when(documentRepository.save(any(DocumentMetadata.class))).thenAnswer(saveInvocation -> saveInvocation.getArgument(0));
            return operation.execute(new CompensationContext());
        });
        DocumentService documentService = createService();
        documentService.saveDocument(command(content));

        verify(contentHasher).sha256(content);

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getContentSha256()).isEqualTo(expectedSha256);
    }

    @Test
    void createsServerGeneratedStorageKey() {
        byte[] content = pdfContent();

        when(contentHasher.sha256(content)).thenReturn("sha256");
        when(storageTransactionManager.execute(any())).thenAnswer(invocation -> {
            StorageOperation<SavedFile> operation = invocation.getArgument(0);
            when(documentRepository.save(any(DocumentMetadata.class))).thenAnswer(saveInvocation -> saveInvocation.getArgument(0));
            return operation.execute(new CompensationContext());
        });

        DocumentService documentService = createService();
        documentService.saveDocument(command(content));

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());

        String storageKey = captor.getValue().getStorageKey();
        assertThat(storageKey).matches("documents/[0-9a-fA-F-]{36}/source\\.pdf");
        assertThat(storageKey).doesNotContain("company-handbook.pdf");
    }

    @Test
    void createsDocumentMetadataCorrectly() {
        byte[] content = pdfContent();
        String sha256 = "sha256";

        when(contentHasher.sha256(content)).thenReturn(sha256);
        when(storageTransactionManager.execute(any())).thenAnswer(invocation -> {
            StorageOperation<SavedFile> operation = invocation.getArgument(0);
            when(documentRepository.save(any(DocumentMetadata.class))).thenAnswer(saveInvocation -> saveInvocation.getArgument(0));
            return operation.execute(new CompensationContext());
        });

        DocumentService documentService = createService();
        documentService.saveDocument(command(content));

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());

        DocumentMetadata metadata = captor.getValue();
        assertThat(metadata.getDocumentId()).isNotNull();
        assertThat(metadata.getTitle()).isEqualTo("company-handbook.pdf");
        assertThat(metadata.getContentType()).isEqualTo("application/pdf");
        assertThat(metadata.getSize()).isEqualTo(content.length);
        assertThat(metadata.getStorageKey()).isNotBlank();
        assertThat(metadata.getContentSha256()).isEqualTo(sha256);
    }

    @Test
    void setsUploadedLifecycleStatus() {
        byte[] content = pdfContent();

        when(contentHasher.sha256(content)).thenReturn("sha256");
        when(storageTransactionManager.execute(any())).thenAnswer(invocation -> {
            StorageOperation<SavedFile> operation = invocation.getArgument(0);
            when(documentRepository.save(any(DocumentMetadata.class))).thenAnswer(saveInvocation -> saveInvocation.getArgument(0));
            return operation.execute(new CompensationContext());
        });

        DocumentService documentService = createService();
        documentService.saveDocument(command(content));

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    void propagatesStorageFailureFromTransactionManager() {
        byte[] content = pdfContent();

        StorageException expected = new StorageException("Storage transaction failed");
        when(storageTransactionManager.execute(any())).thenThrow(expected);

        DocumentService documentService = createService();

        assertThatThrownBy(() -> documentService.saveDocument(command(content)))
            .isSameAs(expected);

        verify(contentHasher).sha256(content);
        verify(storageTransactionManager).execute(any());
        verifyNoInteractions(fileStorage);
        verifyNoInteractions(documentRepository);
    }

    @Test
    void missingDocumentMetadataProducesDocumentNotFoundException() {
        UUID documentId = UUID.randomUUID();

        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.empty());

        DocumentService documentService = createService();

        assertThatThrownBy(() -> documentService.getMetaDataDocument(documentId))
            .isInstanceOf(DocumentNotFoundException.class).hasMessage("Metadata of the document not found!");
        verify(documentRepository).findByDocumentId(documentId);
    }

    @Test
    void retrievesExpectedDocumentMetadata() {
        UUID documentId = UUID.randomUUID();
        DocumentMetadata metadata = DocumentMetadata.builder()
            .id(42L)
            .documentId(documentId)
            .title("company-handbook.pdf")
            .contentType("application/pdf")
            .size(1024L)
            .storageKey("documents/" + documentId + "/source.pdf")
            .status(DocumentStatus.UPLOADED)
            .contentSha256("sha256")
            .build();

        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(metadata));
        DocumentService documentService = createService();
        DocumentMetadata result = documentService.getMetaDataDocument(documentId);

        assertThat(result).isSameAs(metadata);
        assertThat(result.getDocumentId()).isEqualTo(documentId);
        assertThat(result.getTitle()).isEqualTo("company-handbook.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        assertThat(result.getSize()).isEqualTo(1024L);
        assertThat(result.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(result.getContentSha256()).isEqualTo("sha256");
        verify(documentRepository).findByDocumentId(documentId);
    }

    @Test
    void downloadsStoredPdfBytes() {
        UUID documentId = UUID.randomUUID();
        byte[] content = pdfContent();
        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentId(documentId)
            .storageKey("documents/" + documentId + "/source.pdf")
            .build();

        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(metadata));
        when(fileStorage.get(metadata.getStorageKey())).thenReturn(new ByteArrayInputStream(content));

        DocumentService documentService = createService();
        byte[] result = documentService.downloadDocument(documentId);

        assertThat(result).containsExactly(content);
        verify(documentRepository).findByDocumentId(documentId);
        verify(fileStorage).get(metadata.getStorageKey());
    }

    @Test
    void convertsStorageReadFailureToStorageException() throws Exception {
        UUID documentId = UUID.randomUUID();
        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentId(documentId)
            .storageKey("documents/" + documentId + "/source.pdf")
            .build();

        when(documentRepository.findByDocumentId(documentId)).thenReturn(Optional.of(metadata));
        InputStream failingInputStream = mock(InputStream.class);
        when(failingInputStream.readAllBytes()).thenThrow(new IOException("MinIO connection failed"));
        when(fileStorage.get(metadata.getStorageKey())).thenReturn(failingInputStream);

        DocumentService documentService = createService();

        assertThatThrownBy(() -> documentService.downloadDocument(documentId))
            .isInstanceOf(StorageException.class)
            .hasMessage("Failed to read document")
            .hasCauseInstanceOf(IOException.class);

        verify(documentRepository).findByDocumentId(documentId);
        verify(fileStorage).get(metadata.getStorageKey());
    }

    private DocumentService createService() {
        return new DocumentService(fileStorage, contentHasher, documentRepository, storageTransactionManager);
    }

    private UploadDocumentCommand command(byte[] content) {
        return new UploadDocumentCommand("company-handbook.pdf", "application/pdf", content.length, new ByteArrayInputStream(content));
    }

    private byte[] pdfContent() {
        return "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8);
    }
}