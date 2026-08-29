package com.example.ragknowledgeservice;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.TimeZone;

public class TimezoneExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(@NonNull ExtensionContext context) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
