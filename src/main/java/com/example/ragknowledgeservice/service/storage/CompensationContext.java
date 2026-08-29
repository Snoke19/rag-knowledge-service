package com.example.ragknowledgeservice.service.storage;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
public class CompensationContext {

    private final Deque<CompensationAction> actions = new ArrayDeque<>();

    public void register(CompensationAction action) {
        actions.push(action);
    }

    void compensate() {
        while (!actions.isEmpty()) {
            CompensationAction action = actions.pop();

            try {
                action.compensate();
            } catch (Exception exception) {
                log.error("Compensation action failed", exception);
            }
        }
    }
}