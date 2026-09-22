package com.latch.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * High-performance sharded storage engine using lock striping over N shards.
 * Significantly reduces lock contention under multi-threaded client operations.
 */
public class ShardedDatabase implements Database {

    private final Shard[] shards;
    private final int numShards;

    public ShardedDatabase() {
        this(16); // Default 16 shards
    }

    public ShardedDatabase(int numShards) {
        this.numShards = numShards;
        this.shards = new Shard[numShards];
        for (int i = 0; i < numShards; i++) {
            this.shards[i] = new Shard();
        }
    }

    private Shard getShard(String key) {
        int hash = key.hashCode();
        int index = Math.abs(hash % numShards);
        return shards[index];
    }

    @Override
    public Value get(String key) {
        return getShard(key).get(key);
    }

    @Override
    public void set(String key, Value value) {
        getShard(key).set(key, value);
    }

    @Override
    public boolean del(String key) {
        return getShard(key).del(key);
    }

    @Override
    public boolean exists(String key) {
        return getShard(key).exists(key);
    }

    @Override
    public int del(Collection<String> keys) {
        int count = 0;
        for (String k : keys) {
            if (getShard(k).del(k)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Value compute(String key, BiFunction<String, Value, Value> remappingFunction) {
        return getShard(key).compute(key, remappingFunction);
    }

    @Override
    public int size() {
        int total = 0;
        for (Shard shard : shards) {
            total += shard.size();
        }
        return total;
    }

    @Override
    public Map<String, Value> snapshot() {
        Map<String, Value> totalSnapshot = new HashMap<>();
        for (Shard shard : shards) {
            totalSnapshot.putAll(shard.snapshot());
        }
        return totalSnapshot;
    }

    public int getNumShards() {
        return numShards;
    }
}
