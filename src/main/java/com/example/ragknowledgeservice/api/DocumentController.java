package com.example.ragknowledgeservice.api;

import com.example.ragknowledgeservice.common.MultipartFileMapper;
import com.example.ragknowledgeservice.common.ValidPdf;
import com.example.ragknowledgeservice.dto.SavedFile;
import com.example.ragknowledgeservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SavedFile uploadDocument(@RequestPart("file") @ValidPdf MultipartFile file) {
        return documentService.saveDocument(MultipartFileMapper.toFileContent(file));
    }

    @GetMapping("/documents/{id}")
    public String getDocument(@PathVariable String id) {
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
}
