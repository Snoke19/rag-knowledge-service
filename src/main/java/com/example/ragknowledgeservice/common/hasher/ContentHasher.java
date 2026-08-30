package com.example.ragknowledgeservice.common.hasher;

public interface ContentHasher {

    String sha256(byte[] content);
}
