package com.example.ragknowledgeservice.service.storage;

import com.example.ragknowledgeservice.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageTransactionManager {

    private final TransactionTemplate transactionTemplate;

    public <T> T execute(StorageOperation<T> operation) {
        CompensationContext context = new CompensationContext();

        try {
            return transactionTemplate.execute(status -> {
                try {
                    return operation.execute(context);
                } catch (RuntimeException exception) {
                    status.setRollbackOnly();
                    throw exception;
                }
            });
        } catch (Exception exception) {
            context.compensate();

            log.error("Storage transaction failed", exception);

            throw new StorageException("Storage transaction failed", exception);
        }
    }
}
