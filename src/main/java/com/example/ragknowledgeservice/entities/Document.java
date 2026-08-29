package com.example.ragknowledgeservice.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Document {

    private String id;
    private String title;
    private String contentType;
    private long size;
    private String storageKey;
}
