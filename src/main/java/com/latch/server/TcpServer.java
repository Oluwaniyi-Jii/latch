package com.latch.server;

import com.latch.command.CommandRegistry;
import com.latch.storage.Database;
import com.latch.storage.SimpleDatabase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Non-blocking Java NIO TCP Server for Latch database.
 */
public class TcpServer implements Runnable {

    private final int port;
    private final Database database;
    private final CommandRegistry commandRegistry;

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private volatile boolean running = false;

    public TcpServer(int port) {
        this(port, new SimpleDatabase(), new CommandRegistry());
    }

    public TcpServer(int port, Database database, CommandRegistry commandRegistry) {
        this.port = port;
        this.database = database;
        this.commandRegistry = commandRegistry;
    }

    public void start() throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        System.out.println("Latch TCP Server listening on port " + port);
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (selector.select(500) == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection(key);
                    } else if (key.isReadable()) {
                        ClientConnection conn = (ClientConnection) key.attachment();
                        if (conn != null) {
                            conn.handleRead();
                        }
                    } else if (key.isWritable()) {
                        ClientConnection conn = (ClientConnection) key.attachment();
                        if (conn != null) {
                            conn.handleWrite();
                        }
                    }
                }
            } catch (ClosedSelectorException e) {
                break;
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error in Selector event loop: " + e.getMessage());
                }
            }
        }
    }

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);

            ClientConnection connection = new ClientConnection(clientChannel, clientKey, database, commandRegistry);
            clientKey.attach(connection);
        }
    }

    public void stop() {
        running = false;
        if (selector != null) {
            selector.wakeup();
            try {
                selector.close();
            } catch (IOException ignored) {
            }
        }
        if (serverChannel != null) {
            try {
                serverChannel.close();
            } catch (IOException ignored) {
            }
        }
    }

    public int getPort() {
        return port;
    }
}
