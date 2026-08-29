package com.example.ragknowledgeservice.service.storage.file_storage;

import com.example.ragknowledgeservice.config.MinioStorageProperties;
import com.example.ragknowledgeservice.service.storage.StorageException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioFileStorage implements FileStorage {

    private final MinioClient minioClient;
    private final MinioStorageProperties properties;

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            ensureBucketExists();

            ObjectWriteResponse objectWriteResponse = minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .stream(content, size, -1L)
                    .contentType(contentType)
                    .build()
            );

            log.debug("File saved intp MinIO. bucket: {}", objectWriteResponse.bucket());
        } catch (Exception exception) {
            throw new StorageException("Failed to store file: " + key, exception);
        }
    }

    @Override
    public InputStream get(String key) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build()
            );

        } catch (Exception exception) {
            throw new StorageException("Failed to retrieve file: " + key, exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(key)
                    .build()
            );

        } catch (Exception exception) {
            throw new StorageException("Failed to delete file: " + key, exception);
        }
    }

    @Override
    public void deleteAll() {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                .bucket(properties.bucket())
                .build());
        } catch (Exception exception) {
            log.error("Failed to delete all files from bucket: {}", properties.bucket(), exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build()
            );
        }
    }
}
