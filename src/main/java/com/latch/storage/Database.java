package com.latch.storage;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Storage Engine Interface for Latch Database.
 */
public interface Database {
    Value get(String key);

    void set(String key, Value value);

    boolean del(String key);

    boolean exists(String key);

    int del(Collection<String> keys);

    Value compute(String key, BiFunction<String, Value, Value> remappingFunction);

    int size();

    Map<String, Value> snapshot();
}
