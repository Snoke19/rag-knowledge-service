package com.example.ragknowledgeservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documents")
public class DocumentMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private UUID documentId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "storage_key", nullable = false, unique = true, length = 64)
    private String storageKey;
}
