package com.latch.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Record representing a hash map value in Latch storage.
 */
public record HashValue(Map<String, String> value) implements Value {
    public HashValue {
        Objects.requireNonNull(value, "Hash map value cannot be null");
    }

    public HashValue() {
        this(new HashMap<>());
    }

    @Override
    public String typeName() {
        return "hash";
    }
}
