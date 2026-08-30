package com.example.ragknowledgeservice.repositories;

import com.example.ragknowledgeservice.entities.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<DocumentMetadata, Long> {

    Optional<DocumentMetadata> findByDocumentId(UUID documentId);
}
