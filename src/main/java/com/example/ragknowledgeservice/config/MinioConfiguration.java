package com.example.ragknowledgeservice.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(MinioStorageProperties properties) {
        return MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(
                properties.accessKey(),
                properties.secretKey()
            )
            .build();
    }
}
