package com.example.ragknowledgeservice.service.storage.file_storage;

import java.io.InputStream;

public interface FileStorage {

    void put(
        String key,
        InputStream content,
        long size,
        String contentType
    );

    InputStream get(String key);

    void delete(String key);
}
