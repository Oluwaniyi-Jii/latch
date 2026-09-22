package com.latch.storage;

import com.latch.command.CommandRegistry;
import com.latch.protocol.Encoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShardedDatabaseTest {

    private ShardedDatabase db;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        db = new ShardedDatabase(8); // 8 shards for testing
        registry = new CommandRegistry();
    }

    @Test
    @DisplayName("List Commands: LPUSH, LPOP, LRANGE on ShardedDatabase")
    void testListCommands() {
        // LPUSH mylist elem1 elem2
        byte[] lpushResp = registry.execute(List.of("LPUSH", "mylist", "elem1", "elem2"), db);
        assertEquals(":2\r\n", new String(lpushResp, StandardCharsets.UTF_8));

        // LRANGE mylist 0 -1 -> ["elem2", "elem1"]
        byte[] lrangeResp = registry.execute(List.of("LRANGE", "mylist", "0", "-1"), db);
        assertEquals("*2\r\n$5\r\nelem2\r\n$5\r\nelem1\r\n", new String(lrangeResp, StandardCharsets.UTF_8));

        // LPOP mylist -> "elem2"
        byte[] lpopResp = registry.execute(List.of("LPOP", "mylist"), db);
        assertEquals("$5\r\nelem2\r\n", new String(lpopResp, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Hash Commands: HSET, HGET, HDEL, HGETALL on ShardedDatabase")
    void testHashCommands() {
        // HSET user:1 name Alice age 24
        byte[] hsetResp = registry.execute(List.of("HSET", "user:1", "name", "Alice", "age", "24"), db);
        assertEquals(":2\r\n", new String(hsetResp, StandardCharsets.UTF_8));

        // HGET user:1 name -> "Alice"
        byte[] hgetResp = registry.execute(List.of("HGET", "user:1", "name"), db);
        assertEquals("$5\r\nAlice\r\n", new String(hgetResp, StandardCharsets.UTF_8));

        // HDEL user:1 name -> :1
        byte[] hdelResp = registry.execute(List.of("HDEL", "user:1", "name"), db);
        assertEquals(":1\r\n", new String(hdelResp, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Set Commands: SADD, SISMEMBER, SREM, SMEMBERS on ShardedDatabase")
    void testSetCommands() {
        // SADD tags java redis latch
        byte[] saddResp = registry.execute(List.of("SADD", "tags", "java", "redis", "latch"), db);
        assertEquals(":3\r\n", new String(saddResp, StandardCharsets.UTF_8));

        // SISMEMBER tags java -> :1
        byte[] sismemberResp = registry.execute(List.of("SISMEMBER", "tags", "java"), db);
        assertEquals(":1\r\n", new String(sismemberResp, StandardCharsets.UTF_8));

        // SREM tags java -> :1
        byte[] sremResp = registry.execute(List.of("SREM", "tags", "java"), db);
        assertEquals(":1\r\n", new String(sremResp, StandardCharsets.UTF_8));
    }
}
