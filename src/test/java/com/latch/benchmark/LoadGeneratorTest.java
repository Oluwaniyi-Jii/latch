package com.latch.benchmark;

import com.latch.server.TcpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadGeneratorTest {

    private TcpServer server;
    private int port;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        server = new TcpServer(port);
        server.start();
        serverThread = new Thread(server);
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        serverThread.join(2000);
    }

    @Test
    @DisplayName("Benchmark: Measure throughput and P50/P95/P99 latencies under concurrent client load")
    void testLoadGenerator() throws Exception {
        int clientCount = 5;
        int opsPerClient = 100;

        LoadGenerator.BenchmarkResult result = LoadGenerator.runBenchmark("localhost", port, clientCount, opsPerClient, LoadGenerator.Workload.MIXED_50_50);

        assertNotNull(result);
        result.printSummary();

        assertTrue(result.totalOperations > 0);
        assertTrue(result.throughputOpsPerSec > 0);
        assertTrue(result.p50Millis >= 0);
    }
}
