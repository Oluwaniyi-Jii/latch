package com.latch.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe observability metrics registry tracking server statistics.
 */
public class MetricsRegistry {

    private final long startTimeMillis = System.currentTimeMillis();
    private final AtomicLong totalCommandsProcessed = new AtomicLong(0);
    private final AtomicLong activeClientsCount = new AtomicLong(0);
    private final AtomicLong expiredKeysCount = new AtomicLong(0);

    public void incrementCommands() {
        totalCommandsProcessed.incrementAndGet();
    }

    public void clientConnected() {
        activeClientsCount.incrementAndGet();
    }

    public void clientDisconnected() {
        activeClientsCount.decrementAndGet();
    }

    public void keyExpired() {
        expiredKeysCount.incrementAndGet();
    }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startTimeMillis) / 1000L;
    }

    public long getTotalCommandsProcessed() {
        return totalCommandsProcessed.get();
    }

    public long getActiveClientsCount() {
        return Math.max(0, activeClientsCount.get());
    }

    public long getExpiredKeysCount() {
        return expiredKeysCount.get();
    }

    public long getUsedMemoryBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
