package com.example.ragknowledgeservice.repositories;

import com.example.ragknowledgeservice.entities.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentMetadata, String> {

    DocumentMetadata findByDocumentId(UUID documentId);
}
