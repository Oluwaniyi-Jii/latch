package com.latch.storage;

import java.util.Objects;

/**
 * Record representing a string value in Latch storage.
 */
public record StringValue(String value) implements Value {
    public StringValue {
        Objects.requireNonNull(value, "String value cannot be null");
    }

    @Override
    public String typeName() {
        return "string";
    }
}
