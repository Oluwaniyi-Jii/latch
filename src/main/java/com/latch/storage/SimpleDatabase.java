package com.latch.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Basic ConcurrentHashMap storage engine implementation for core testing.
 */
public class SimpleDatabase implements Database {
    private final ConcurrentHashMap<String, Value> store = new ConcurrentHashMap<>();

    @Override
    public Value get(String key) {
        return store.get(key);
    }

    @Override
    public void set(String key, Value value) {
        store.put(key, value);
    }

    @Override
    public boolean del(String key) {
        return store.remove(key) != null;
    }

    @Override
    public boolean exists(String key) {
        return store.containsKey(key);
    }

    @Override
    public int del(Collection<String> keys) {
        int count = 0;
        for (String k : keys) {
            if (store.remove(k) != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Value compute(String key, BiFunction<String, Value, Value> remappingFunction) {
        return store.compute(key, remappingFunction);
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public Map<String, Value> snapshot() {
        return new HashMap<>(store);
    }
}
