package com.example.ragknowledgeservice.service;

import com.example.ragknowledgeservice.command.UploadDocumentCommand;
import com.example.ragknowledgeservice.common.DocumentStatus;
import com.example.ragknowledgeservice.config.MinioStorageProperties;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.entities.DocumentMetadata;
import com.example.ragknowledgeservice.repositories.DocumentRepository;
import com.example.ragknowledgeservice.service.storage.StorageException;
import com.example.ragknowledgeservice.service.storage.StorageTransactionManager;
import com.example.ragknowledgeservice.service.storage.file_storage.FileStorage;
import com.example.ragknowledgeservice.service.storage.file_storage.MinioFileStorage;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.example.ragknowledgeservice=DEBUG"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DocumentServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest");

    @Autowired
    private DocumentService documentService;

    @Autowired
    private MinioClient minioClient;

    @MockitoSpyBean
    private FileStorage fileStorage;

    @MockitoSpyBean
    private DocumentRepository documentRepository;

    @BeforeEach
    public void setup() {
        documentRepository.deleteAll();
        documentService.deleteAll();

        Mockito.reset(fileStorage, documentRepository);
    }

    @Test
    void test_save_document_success() {
        String content = "test document content";

        SavedFile result = documentService.saveDocument(new UploadDocumentCommand(
            "test.pdf",
            "application/pdf",
            content.length(),
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        ));

        assertNotNull(result);
        assertNotNull(result.getDocumentId());
        assertEquals(DocumentStatus.UPLOADED, result.getStatus());

        DocumentMetadata documentMetadata = documentService.getMetaDataDocument(result.getDocumentId());
        assertNotNull(documentMetadata);
        assertNotNull(documentMetadata.getId());
        assertEquals(documentMetadata.getDocumentId(), result.getDocumentId());
        assertEquals(DocumentStatus.UPLOADED, result.getStatus());

        byte[] downloadedDocument = documentService.downloadDocument(result.getDocumentId());
        String documentContent = new String(downloadedDocument, StandardCharsets.UTF_8);
        assertEquals(content, documentContent);
    }

    @Test
    void saveDocument_shouldNotPersistMetadataWhenStorageFails() {
        String content = "test document content";

        doThrow(new StorageException("Storage failure"))
            .when(fileStorage)
            .put(any(), any(), anyLong(), any());

        UploadDocumentCommand command = new UploadDocumentCommand(
            "test.pdf",
            "application/pdf",
            content.length(),
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        assertThrows(StorageException.class, () -> documentService.saveDocument(command));

        assertTrue(documentRepository.findAll().isEmpty());
        verify(fileStorage).put(
            anyString(),
            any(InputStream.class),
            eq((long) content.length()),
            eq("application/pdf")
        );
        verify(documentRepository, never()).save(any(DocumentMetadata.class));
        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    void saveDocument_shouldDeleteStoredObjectWhenDatabaseFails() {
        String content = "test document content";

        doThrow(new RuntimeException("Database failure"))
            .when(documentRepository)
            .save(any(DocumentMetadata.class));

        UploadDocumentCommand command = new UploadDocumentCommand(
            "test.pdf",
            "application/pdf",
            content.length(),
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        StorageException exception = assertThrows(StorageException.class, () -> documentService.saveDocument(command));

        assertEquals("Storage transaction failed", exception.getMessage());

        assertTrue(documentRepository.findAll().isEmpty());

        ArgumentCaptor<DocumentMetadata> captor = ArgumentCaptor.forClass(DocumentMetadata.class);
        verify(documentRepository).save(captor.capture());

        DocumentMetadata attemptedDocument = captor.getValue();
        String storageKey = attemptedDocument.getStorageKey();
        verify(fileStorage).delete(storageKey);
        assertObjectDoesNotExist(storageKey);
    }

    @Test
    void saveDocument_shouldKeepDatabaseFailureWhenCompensationFails() {
        String content = "test document content";

        RuntimeException databaseFailure = new RuntimeException("Database failure");

        doThrow(databaseFailure)
            .when(documentRepository)
            .save(any(DocumentMetadata.class));

        doThrow(new StorageException("Delete failure"))
            .when(fileStorage)
            .delete(anyString());

        UploadDocumentCommand command = new UploadDocumentCommand(
            "test.pdf",
            "application/pdf",
            content.length(),
            new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
        );

        StorageException exception = assertThrows(StorageException.class, () -> documentService.saveDocument(command));

        assertEquals("Storage transaction failed", exception.getMessage());
        assertEquals(databaseFailure, exception.getCause());
        verify(documentRepository).save(any(DocumentMetadata.class));
        verify(fileStorage).delete(anyString());
    }

    private void assertObjectDoesNotExist(String storageKey) {
        assertThrows(ErrorResponseException.class, () -> minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket("test-bucket")
                    .object(storageKey)
                    .build()
            )
        );
    }

    @TestConfiguration
    static class DocumentServiceTestConfig {

        @Bean
        public MinioClient minioClient() {
            return MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials(minio.getUserName(), minio.getPassword())
                .build();
        }

        @Bean
        public MinioStorageProperties minioStorageProperties() {
            return new MinioStorageProperties("", "", "", "test-bucket");
        }

        @Bean
        public FileStorage fileStorage(MinioClient minioClient, MinioStorageProperties minioStorageProperties) {
            return new MinioFileStorage(minioClient, minioStorageProperties);
        }

        @Bean
        public DocumentService documentService(FileStorage fileStorage,
                                               DocumentRepository documentRepository,
                                               StorageTransactionManager storageTransactionManager) {
            return new DocumentService(fileStorage, documentRepository, storageTransactionManager);
        }

        @Bean
        public StorageTransactionManager storageTransactionManager(TransactionTemplate transactionTemplate) {
            return new StorageTransactionManager(transactionTemplate);
        }
    }
}
