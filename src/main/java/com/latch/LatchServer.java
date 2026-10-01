package com.latch;

import com.latch.server.TcpServer;

import java.io.IOException;

public class LatchServer {
    public static void main(String[] args) {
        int port = 6380;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        TcpServer server = new TcpServer(port);
        try {
            server.start();

            Thread serverThread = new Thread(server, "Latch-NIO-EventLoop");
            serverThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Stopping Latch Server...");
                server.stop();
            }));

            serverThread.join();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to run Latch Server: " + e.getMessage());
        }
    }
}
