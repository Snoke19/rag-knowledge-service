package com.example.ragknowledgeservice.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    @PostMapping("/chat")
    public String chatDocuments() {
        return "document";
    }
}
