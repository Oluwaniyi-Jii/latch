package com.latch.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TcpServerTest {

    private TcpServer server;
    private int port;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        // Find free ephemeral port
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
    @DisplayName("TCP Integration: PING -> +PONG\\r\\n")
    void testPingOverTcp() throws Exception {
        try (Socket socket = new Socket("localhost", port);
             OutputStream out = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            out.write("PING\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            String response = reader.readLine();
            assertEquals("+PONG", response);
        }
    }

    @Test
    @DisplayName("TCP Integration: SET, GET, EXISTS, DEL workflow")
    void testStorageCommandsOverTcp() throws Exception {
        try (Socket socket = new Socket("localhost", port);
             OutputStream out = socket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. SET name alice
            out.write("*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nalice\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("+OK", reader.readLine());

            // 2. GET name
            out.write("*2\r\n$3\r\nGET\r\n$4\r\nname\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("$5", reader.readLine());
            assertEquals("alice", reader.readLine());

            // 3. EXISTS name
            out.write("*2\r\n$6\r\nEXISTS\r\n$4\r\nname\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals(":1", reader.readLine());

            // 4. DEL name
            out.write("*2\r\n$3\r\nDEL\r\n$4\r\nname\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals(":1", reader.readLine());

            // 5. GET name after deletion
            out.write("*2\r\n$3\r\nGET\r\n$4\r\nname\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("$-1", reader.readLine());
        }
    }
}
