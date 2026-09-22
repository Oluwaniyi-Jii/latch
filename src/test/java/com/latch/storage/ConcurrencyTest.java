package com.latch.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrencyTest {

    private ShardedDatabase db;

    @BeforeEach
    void setUp() {
        db = new ShardedDatabase(16);
    }

    @Test
    @DisplayName("Concurrency: concurrentSetOperations across multiple threads")
    void concurrentSetOperations() throws Exception {
        int threads = 10;
        int keysPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < keysPerThread; i++) {
                        db.set("key:" + threadId + ":" + i, new StringValue("val-" + i));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threads * keysPerThread, db.size());
    }

    @Test
    @DisplayName("Concurrency: concurrentReads without locking interference")
    void concurrentReads() throws Exception {
        for (int i = 0; i < 500; i++) {
            db.set("shared-key:" + i, new StringValue("shared-val-" + i));
        }

        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 500; i++) {
                        Value val = db.get("shared-key:" + i);
                        if (val instanceof StringValue s && s.value().equals("shared-val-" + i)) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threads * 500, successCount.get());
    }

    @Test
    @DisplayName("Concurrency: readWriteContention concurrent readers and writers")
    void readWriteContention() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 500; i++) {
                        String key = "contention-key:" + (i % 50);
                        if (threadId % 2 == 0) {
                            db.set(key, new StringValue("writer-" + threadId + "-" + i));
                        } else {
                            db.get(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    @DisplayName("Concurrency: deleteWhileReading safety check")
    void deleteWhileReading() throws Exception {
        String key = "delete-test-key";
        db.set(key, new StringValue("initial-val"));

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        // 3 Readers
        for (int i = 0; i < 3; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        db.get(key); // Should never throw NullPointerException or ConcurrentModificationException
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 1 Deleter
        executor.submit(() -> {
            try {
                Thread.sleep(10);
                db.del(key);
            } catch (InterruptedException ignored) {
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertFalse(db.exists(key));
    }
}
