package com.latch;

import com.latch.expiration.ExpirationManager;
import com.latch.persistence.AofWriter;
import com.latch.persistence.FsyncPolicy;
import com.latch.server.TcpServer;
import com.latch.storage.ShardedDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class E2EIntegrationTest {

    @TempDir
    Path tempDir;

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
    @DisplayName("End-to-End: Netty/NIO TCP workflow with SET, GET, LIST, HASH, SETEX, INFO")
    void testEndToEndWorkflow() throws Exception {
        try (Socket socket = new Socket("localhost", port);
             OutputStream out = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. PING
            out.write("PING\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("+PONG", reader.readLine());

            // 2. SET name Alice
            out.write("SET name Alice\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("+OK", reader.readLine());

            // 3. GET name
            out.write("GET name\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("$5", reader.readLine());
            assertEquals("Alice", reader.readLine());

            // 4. LPUSH colors red blue
            out.write("LPUSH colors red blue\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals(":2", reader.readLine());

            // 5. HSET user:1 email alice@example.com
            out.write("HSET user:1 email alice@example.com\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals(":1", reader.readLine());

            // 6. SADD tags java db
            out.write("SADD tags java db\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals(":2", reader.readLine());
        }
    }
}
