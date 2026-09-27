package com.latch.expiration;

import com.latch.storage.Database;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Key Expiration Subsystem.
 * Combines passive expiration on key access with active min-heap (PriorityQueue) eviction
 * driven by a ScheduledExecutorService.
 */
public class ExpirationManager {

    private final Database db;
    private final ConcurrentHashMap<String, Long> ttlMap = new ConcurrentHashMap<>();
    private final PriorityQueue<ExpirationEntry> minHeap = new PriorityQueue<>();
    private final ReentrantLock heapLock = new ReentrantLock();

    private final ScheduledExecutorService scheduler;
    private boolean activeCleanupRunning = false;

    public ExpirationManager(Database db) {
        this.db = db;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Latch-Expiration-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void setExpirySeconds(String key, long seconds) {
        setExpiryEpochMilli(key, System.currentTimeMillis() + (seconds * 1000L));
    }

    public void setExpiryEpochMilli(String key, long epochMilli) {
        ttlMap.put(key, epochMilli);
        heapLock.lock();
        try {
            minHeap.add(new ExpirationEntry(key, epochMilli));
        } finally {
            heapLock.unlock();
        }
    }

    public long getTtlSeconds(String key) {
        if (!db.exists(key)) {
            return -2; // Key does not exist
        }
        Long expireAt = ttlMap.get(key);
        if (expireAt == null) {
            return -1; // Key exists but has no associated expire
        }

        long now = System.currentTimeMillis();
        if (now >= expireAt) {
            evictKey(key);
            return -2;
        }

        long remainingSec = (expireAt - now) / 1000L;
        return Math.max(0, remainingSec);
    }

    public boolean persist(String key) {
        Long removed = ttlMap.remove(key);
        return removed != null;
    }

    /**
     * Passive eviction check called on key access (e.g. GET/HGET/etc).
     * Returns true if the key was expired and evicted.
     */
    public boolean checkPassive(String key) {
        Long expireAt = ttlMap.get(key);
        if (expireAt != null && System.currentTimeMillis() >= expireAt) {
            evictKey(key);
            return true;
        }
        return false;
    }

    public synchronized void startActiveCleanup() {
        if (!activeCleanupRunning) {
            activeCleanupRunning = true;
            scheduler.scheduleWithFixedDelay(this::cleanExpiredKeys, 100, 100, TimeUnit.MILLISECONDS);
        }
    }

    public void cleanExpiredKeys() {
        long now = System.currentTimeMillis();
        heapLock.lock();
        try {
            while (!minHeap.isEmpty()) {
                ExpirationEntry head = minHeap.peek();
                if (head.getExpireAtEpochMilli() <= now) {
                    minHeap.poll();
                    String key = head.getKey();
                    Long registeredExpiry = ttlMap.get(key);
                    // Check if current heap entry matches registered expiry (handles TTL reset/overwrites)
                    if (registeredExpiry != null && registeredExpiry == head.getExpireAtEpochMilli()) {
                        evictKey(key);
                    }
                } else {
                    break;
                }
            }
        } finally {
            heapLock.unlock();
        }
    }

    private void evictKey(String key) {
        ttlMap.remove(key);
        db.del(key);
    }

    public void stopActiveCleanup() {
        scheduler.shutdownNow();
    }

    public int getExpiringKeysCount() {
        return ttlMap.size();
    }
}
