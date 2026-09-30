package com.latch.transaction;

import com.latch.command.CommandRegistry;
import com.latch.command.TxCommands;
import com.latch.protocol.Encoder;
import com.latch.storage.ShardedDatabase;
import com.latch.storage.StringValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    private ShardedDatabase db;
    private CommandRegistry registry;
    private TransactionManager txManager;
    private TxCommands.TransactionState clientTxState;

    @BeforeEach
    void setUp() {
        db = new ShardedDatabase(8);
        txManager = new TransactionManager();
        clientTxState = new TxCommands.TransactionState();
        registry = new CommandRegistry();

        registry.register("MULTI", new TxCommands.MultiCommand(clientTxState));
        registry.register("DISCARD", new TxCommands.DiscardCommand(clientTxState));
        registry.register("WATCH", new TxCommands.WatchCommand(clientTxState, txManager));
        registry.register("UNWATCH", new TxCommands.UnwatchCommand(clientTxState));
        registry.register("EXEC", new TxCommands.ExecCommand(clientTxState, txManager, registry));
    }

    @Test
    @DisplayName("Transaction: MULTI -> SET -> SET -> EXEC atomic execution")
    void testBasicMultiExec() {
        byte[] multiResp = registry.execute(List.of("MULTI"), db);
        assertEquals("+OK\r\n", new String(multiResp, StandardCharsets.UTF_8));
        assertTrue(clientTxState.isInMulti());

        clientTxState.getQueuedCommands().add(List.of("SET", "balance", "100"));
        clientTxState.getQueuedCommands().add(List.of("SET", "status", "active"));

        byte[] execResp = registry.execute(List.of("EXEC"), db);
        assertEquals("*2\r\n+OK\r\n+OK\r\n", new String(execResp, StandardCharsets.UTF_8));

        assertFalse(clientTxState.isInMulti());
        assertEquals(new StringValue("100"), db.get("balance"));
        assertEquals(new StringValue("active"), db.get("status"));
    }

    @Test
    @DisplayName("Transaction: DISCARD resets queued commands and MULTI state")
    void testDiscard() {
        registry.execute(List.of("MULTI"), db);
        clientTxState.getQueuedCommands().add(List.of("SET", "foo", "bar"));

        byte[] discardResp = registry.execute(List.of("DISCARD"), db);
        assertEquals("+OK\r\n", new String(discardResp, StandardCharsets.UTF_8));

        assertFalse(clientTxState.isInMulti());
        assertNull(db.get("foo"));
    }

    @Test
    @DisplayName("Transaction: WATCH aborts EXEC when watched key is modified by another client")
    void testWatchAbortOnConcurrentModification() {
        db.set("balance", new StringValue("100"));
        txManager.incrementVersion("balance");

        // Client A watches 'balance'
        registry.execute(List.of("WATCH", "balance"), db);

        // Client A calls MULTI and queues SET balance 200
        registry.execute(List.of("MULTI"), db);
        clientTxState.getQueuedCommands().add(List.of("SET", "balance", "200"));

        // Client B concurrently modifies 'balance' to 150
        db.set("balance", new StringValue("150"));
        txManager.incrementVersion("balance");

        // Client A calls EXEC -> fails due to version mismatch!
        byte[] execResp = registry.execute(List.of("EXEC"), db);
        assertEquals("*-1\r\n", new String(execResp, StandardCharsets.UTF_8), "Transaction must abort with null array");

        // Verify balance was NOT overwritten by Client A
        assertEquals(new StringValue("150"), db.get("balance"));
    }
}
