package com.example.ragknowledgeservice.repositories;

import com.example.ragknowledgeservice.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
}
