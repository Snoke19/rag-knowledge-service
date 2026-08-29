package com.example.ragknowledgeservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.minio")
public record MinioStorageProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucket
) {
}