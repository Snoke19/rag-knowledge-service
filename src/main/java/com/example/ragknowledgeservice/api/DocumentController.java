package com.example.ragknowledgeservice.api;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DocumentController {

    @GetMapping("/documents")
    public List<String> getDocuments() {
        return new ArrayList<>();
    }

    @GetMapping("/documents/{id}")
    public String getDocuments(@PathVariable String id) {
        return "document: " + id;
    }

    @PostMapping("/documents/{id}/ingest")
    public String ingestDocument(@PathVariable String id) {
        return "document ingest: " + id;
    }

    @PostMapping("/search")
    public String searchDocuments() {
        return "document";
    }

    @PostMapping("/chat")
    public String chatDocuments() {
        return "document";
    }
}
