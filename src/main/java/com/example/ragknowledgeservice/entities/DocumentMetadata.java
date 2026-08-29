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

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "title")
    private String title;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "size")
    private long size;

    @Column(name = "storage_key")
    private String storageKey;
}
