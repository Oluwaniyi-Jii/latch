package com.latch.persistence;

import com.latch.command.CommandRegistry;
import com.latch.storage.ShardedDatabase;
import com.latch.storage.StringValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceTest {

    @TempDir
    Path tempDir;

    private ShardedDatabase db;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        db = new ShardedDatabase(8);
        registry = new CommandRegistry();
    }

    @Test
    @DisplayName("Persistence: AOF Append and Log Replay")
    void testAofLogAppendAndReplay() throws Exception {
        Path aofFile = tempDir.resolve("appendonly.log");

        try (AofWriter aofWriter = new AofWriter(aofFile, FsyncPolicy.ALWAYS)) {
            aofWriter.append(List.of("SET", "name", "Alice"));
            aofWriter.append(List.of("SET", "age", "24"));
            aofWriter.append(List.of("SET", "oldKey", "toDelete"));
            aofWriter.append(List.of("DEL", "oldKey"));
        }

        assertTrue(Files.exists(aofFile));

        // Simulate reboot with fresh database
        ShardedDatabase freshDb = new ShardedDatabase(8);
        int replayedCount = AofReader.replayLog(aofFile, freshDb, registry);

        assertEquals(4, replayedCount);
        assertEquals(new StringValue("Alice"), freshDb.get("name"));
        assertEquals(new StringValue("24"), freshDb.get("age"));
        assertNull(freshDb.get("oldKey"));
    }

    @Test
    @DisplayName("Persistence: Atomic Snapshot Save and Load")
    void testAtomicSnapshotSaveAndLoad() throws Exception {
        db.set("user:100", new StringValue("Bob"));
        db.set("user:101", new StringValue("Charlie"));

        Path snapshotFile = tempDir.resolve("dump.rdb");
        SnapshotManager.saveSnapshot(db, snapshotFile);

        assertTrue(Files.exists(snapshotFile));
        assertFalse(Files.exists(tempDir.resolve("dump.rdb.tmp")), "Temp file must be cleanly renamed");

        ShardedDatabase restoredDb = new ShardedDatabase(8);
        int loadedCount = SnapshotManager.loadSnapshot(restoredDb, snapshotFile, registry);

        assertEquals(2, loadedCount);
        assertEquals(new StringValue("Bob"), restoredDb.get("user:100"));
        assertEquals(new StringValue("Charlie"), restoredDb.get("user:101"));
    }

    @Test
    @DisplayName("Persistence: Simulated Crash Recovery")
    void testSimulatedCrashRecovery() throws Exception {
        Path aofFile = tempDir.resolve("crash_recovery.log");

        // 1. Initial server session before crash
        try (AofWriter aofWriter = new AofWriter(aofFile, FsyncPolicy.ALWAYS)) {
            aofWriter.append(List.of("SET", "balance", "100"));
            aofWriter.append(List.of("SET", "status", "active"));
        }

        // 2. Crash occurrs... simulate restart by opening clean DB instance
        ShardedDatabase recoveredDb = new ShardedDatabase(8);
        AofReader.replayLog(aofFile, recoveredDb, registry);

        assertEquals(new StringValue("100"), recoveredDb.get("balance"));
        assertEquals(new StringValue("active"), recoveredDb.get("status"));
    }
}
