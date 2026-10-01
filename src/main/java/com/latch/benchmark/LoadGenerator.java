package com.latch.benchmark;

import org.HdrHistogram.Histogram;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom multi-threaded benchmark client measuring throughput and P50/P95/P99 latencies.
 */
public class LoadGenerator {

    public enum Workload {
        GET_ONLY,
        SET_ONLY,
        MIXED_50_50
    }

    public static class BenchmarkResult {
        public final int clientCount;
        public final Workload workload;
        public final long durationMillis;
        public final long totalOperations;
        public final double throughputOpsPerSec;
        public final double p50Millis;
        public final double p95Millis;
        public final double p99Millis;

        public BenchmarkResult(int clientCount, Workload workload, long durationMillis, long totalOperations,
                               double throughputOpsPerSec, double p50Millis, double p95Millis, double p99Millis) {
            this.clientCount = clientCount;
            this.workload = workload;
            this.durationMillis = durationMillis;
            this.totalOperations = totalOperations;
            this.throughputOpsPerSec = throughputOpsPerSec;
            this.p50Millis = p50Millis;
            this.p95Millis = p95Millis;
            this.p99Millis = p99Millis;
        }

        public void printSummary() {
            System.out.printf("--- Benchmark Summary [%d Clients, Workload: %s] ---%n", clientCount, workload);
            System.out.printf("  Total Ops     : %d%n", totalOperations);
            System.out.printf("  Duration      : %d ms%n", durationMillis);
            System.out.printf("  Throughput    : %.2f ops/sec%n", throughputOpsPerSec);
            System.out.printf("  P50 Latency   : %.3f ms%n", p50Millis);
            System.out.printf("  P95 Latency   : %.3f ms%n", p95Millis);
            System.out.printf("  P99 Latency   : %.3f ms%n", p99Millis);
            System.out.println("---------------------------------------------------------");
        }
    }

    public static BenchmarkResult runBenchmark(String host, int port, int clientCount, int opsPerClient, Workload workload) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(clientCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(clientCount);

        AtomicLong totalOpsCounter = new AtomicLong(0);
        Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(10), 3); // Up to 10s max latency tracking

        byte[] setCommandBytes = "*3\r\n$3\r\nSET\r\n$4\r\nkey1\r\n$5\r\nval10\r\n".getBytes(StandardCharsets.UTF_8);
        byte[] getCommandBytes = "*2\r\n$3\r\nGET\r\n$4\r\nkey1\r\n".getBytes(StandardCharsets.UTF_8);

        for (int c = 0; c < clientCount; c++) {
            final int clientId = c;
            executor.submit(() -> {
                try (Socket socket = new Socket(host, port);
                     OutputStream out = socket.getOutputStream();
                     InputStream in = socket.getInputStream()) {

                    byte[] buffer = new byte[4096];
                    startLatch.await(); // Synchronized start across all worker threads

                    for (int i = 0; i < opsPerClient; i++) {
                        boolean isSet = switch (workload) {
                            case SET_ONLY -> true;
                            case GET_ONLY -> false;
                            case MIXED_50_50 -> (i % 2 == 0);
                        };

                        byte[] payload = isSet ? setCommandBytes : getCommandBytes;

                        long startNano = System.nanoTime();
                        out.write(payload);
                        out.flush();
                        int read = in.read(buffer);
                        long endNano = System.nanoTime();

                        if (read > 0) {
                            long latencyNano = Math.max(1, endNano - startNano);
                            synchronized (histogram) {
                                histogram.recordValue(latencyNano);
                            }
                            totalOpsCounter.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Client error in LoadGenerator: " + e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        long startTime = System.currentTimeMillis();
        startLatch.countDown(); // Fire start signal!
        finishLatch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        long durationMs = Math.max(1, endTime - startTime);
        long totalOps = totalOpsCounter.get();
        double throughput = (totalOps * 1000.0) / durationMs;

        double p50Ms = histogram.getValueAtPercentile(50.0) / 1_000_000.0;
        double p95Ms = histogram.getValueAtPercentile(95.0) / 1_000_000.0;
        double p99Ms = histogram.getValueAtPercentile(99.0) / 1_000_000.0;

        return new BenchmarkResult(clientCount, workload, durationMs, totalOps, throughput, p50Ms, p95Ms, p99Ms);
    }
}
