package com.example.ragknowledgeservice.service.storage;

@FunctionalInterface
public interface StorageOperation<T> {

    T execute(CompensationContext context);
}
