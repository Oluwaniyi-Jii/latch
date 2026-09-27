package com.latch.expiration;

import com.latch.storage.ShardedDatabase;
import com.latch.storage.StringValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpirationManagerTest {

    private ShardedDatabase db;
    private ExpirationManager expirationManager;

    @BeforeEach
    void setUp() {
        db = new ShardedDatabase(8);
        expirationManager = new ExpirationManager(db);
    }

    @AfterEach
    void tearDown() {
        expirationManager.stopActiveCleanup();
    }

    @Test
    @DisplayName("Expiration: Key expires after TTL")
    void testBasicExpiration() throws Exception {
        db.set("temp-key", new StringValue("temp-val"));
        expirationManager.setExpiryEpochMilli("temp-key", System.currentTimeMillis() + 100);

        assertTrue(db.exists("temp-key"));
        Thread.sleep(150);

        // Passive check
        assertTrue(expirationManager.checkPassive("temp-key"));
        assertFalse(db.exists("temp-key"));
    }

    @Test
    @DisplayName("Expiration: TTL and PERSIST operations")
    void testTtlAndPersist() {
        db.set("persisted-key", new StringValue("val"));
        assertEquals(-1, expirationManager.getTtlSeconds("persisted-key"));

        expirationManager.setExpirySeconds("persisted-key", 10);
        assertTrue(expirationManager.getTtlSeconds("persisted-key") > 0);

        assertTrue(expirationManager.persist("persisted-key"));
        assertEquals(-1, expirationManager.getTtlSeconds("persisted-key"));
    }

    @Test
    @DisplayName("Expiration: Active eviction background thread cleans expired keys")
    void testActiveCleanup() throws Exception {
        expirationManager.startActiveCleanup();

        db.set("auto-key", new StringValue("auto-val"));
        expirationManager.setExpiryEpochMilli("auto-key", System.currentTimeMillis() + 50);

        assertTrue(db.exists("auto-key"));
        Thread.sleep(250);

        assertFalse(db.exists("auto-key"));
    }

    @Test
    @DisplayName("Expiration: High key churn with thousands of expiring keys")
    void testHighKeyChurnExpiration() throws Exception {
        expirationManager.startActiveCleanup();

        int keyCount = 2000;
        long now = System.currentTimeMillis();

        for (int i = 0; i < keyCount; i++) {
            String key = "churn-key:" + i;
            db.set(key, new StringValue("val-" + i));
            expirationManager.setExpiryEpochMilli(key, now + 100);
        }

        assertEquals(keyCount, db.size());
        Thread.sleep(400);

        assertEquals(0, db.size(), "All expired keys should be actively cleaned up by ExpirationManager");
    }
}
