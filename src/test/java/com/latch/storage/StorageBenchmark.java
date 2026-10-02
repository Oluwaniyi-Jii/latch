package com.latch.storage;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@Threads(8)
public class StorageBenchmark {

    private ShardedDatabase shardedDb;
    private SimpleDatabase simpleDb;

    @Setup
    public void setup() {
        shardedDb = new ShardedDatabase(16);
        simpleDb = new SimpleDatabase();

        for (int i = 0; i < 1000; i++) {
            shardedDb.set("key:" + i, new StringValue("val:" + i));
            simpleDb.set("key:" + i, new StringValue("val:" + i));
        }
    }

    @Benchmark
    public Value benchmarkShardedDbGet() {
        int index = (int) (System.nanoTime() % 1000);
        return shardedDb.get("key:" + index);
    }

    @Benchmark
    public Value benchmarkSimpleDbGet() {
        int index = (int) (System.nanoTime() % 1000);
        return simpleDb.get("key:" + index);
    }

    @Benchmark
    public void benchmarkShardedDbSet() {
        int index = (int) (System.nanoTime() % 1000);
        shardedDb.set("key:" + index, new StringValue("updated"));
    }

    @Benchmark
    public void benchmarkSimpleDbSet() {
        int index = (int) (System.nanoTime() % 1000);
        simpleDb.set("key:" + index, new StringValue("updated"));
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(StorageBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
