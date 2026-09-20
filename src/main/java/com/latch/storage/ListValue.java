package com.latch.storage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Record representing a list value in Latch storage.
 */
public record ListValue(Deque<String> value) implements Value {
    public ListValue {
        Objects.requireNonNull(value, "List value deque cannot be null");
    }

    public ListValue() {
        this(new ArrayDeque<>());
    }

    @Override
    public String typeName() {
        return "list";
    }
}
