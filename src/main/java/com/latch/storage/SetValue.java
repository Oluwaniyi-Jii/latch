package com.latch.storage;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Record representing a unique set value in Latch storage.
 */
public record SetValue(Set<String> value) implements Value {
    public SetValue {
        Objects.requireNonNull(value, "Set value cannot be null");
    }

    public SetValue() {
        this(new HashSet<>());
    }

    @Override
    public String typeName() {
        return "set";
    }
}
