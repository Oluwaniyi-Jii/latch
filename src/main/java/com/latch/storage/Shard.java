package com.latch.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * Individual storage shard protected by a dedicated ReentrantReadWriteLock.
 */
public class Shard {
    private final Map<String, Value> store = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public Value get(String key) {
        rwLock.readLock().lock();
        try {
            return store.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void set(String key, Value value) {
        rwLock.writeLock().lock();
        try {
            store.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean del(String key) {
        rwLock.writeLock().lock();
        try {
            return store.remove(key) != null;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean exists(String key) {
        rwLock.readLock().lock();
        try {
            return store.containsKey(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Value compute(String key, BiFunction<String, Value, Value> remappingFunction) {
        rwLock.writeLock().lock();
        try {
            Value current = store.get(key);
            Value updated = remappingFunction.apply(key, current);
            if (updated != null) {
                store.put(key, updated);
            } else {
                store.remove(key);
            }
            return updated;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            return store.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<String, Value> snapshot() {
        rwLock.readLock().lock();
        try {
            return new HashMap<>(store);
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
