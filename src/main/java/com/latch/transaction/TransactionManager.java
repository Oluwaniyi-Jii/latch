package com.latch.transaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages version counters for keys to support optimistic concurrency control (WATCH / UNWATCH).
 */
public class TransactionManager {

    private final ConcurrentHashMap<String, AtomicLong> keyVersions = new ConcurrentHashMap<>();

    public long getKeyVersion(String key) {
        AtomicLong version = keyVersions.get(key);
        return version != null ? version.get() : 0L;
    }

    public void incrementVersion(String key) {
        keyVersions.computeIfAbsent(key, k -> new AtomicLong(0L)).incrementAndGet();
    }

    public boolean isWatchedKeyModified(Map<String, Long> watchedKeys) {
        if (watchedKeys == null || watchedKeys.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, Long> entry : watchedKeys.entrySet()) {
            String key = entry.getKey();
            long expectedVersion = entry.getValue();
            long currentVersion = getKeyVersion(key);

            if (currentVersion != expectedVersion) {
                return true; // Key was modified by another client session
            }
        }

        return false;
    }
}
