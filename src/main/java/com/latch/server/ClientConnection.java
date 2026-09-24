package com.latch.server;

import com.latch.command.CommandRegistry;
import com.latch.protocol.Decoder;
import com.latch.storage.Database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Manages state, buffers, decoding, and queued responses for a client NIO SocketChannel.
 */
public class ClientConnection {

    private final SocketChannel channel;
    private final SelectionKey key;
    private final Database database;
    private final CommandRegistry commandRegistry;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(16384); // 16KB read buffer
    private final Queue<ByteBuffer> writeQueue = new ArrayDeque<>();

    public ClientConnection(SocketChannel channel, SelectionKey key, Database database, CommandRegistry commandRegistry) {
        this.channel = channel;
        this.key = key;
        this.database = database;
        this.commandRegistry = commandRegistry;
    }

    public void handleRead() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            close();
            return;
        }

        readBuffer.flip();

        while (readBuffer.hasRemaining()) {
            List<String> commandArgs = Decoder.decode(readBuffer);
            if (commandArgs == null) {
                // Incomplete frame, compact remaining buffer and wait for more data
                break;
            }

            byte[] responseBytes = commandRegistry.execute(commandArgs, database);
            if (responseBytes != null && responseBytes.length > 0) {
                writeQueue.add(ByteBuffer.wrap(responseBytes));
            }
        }

        readBuffer.compact();

        if (!writeQueue.isEmpty()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    public void handleWrite() throws IOException {
        while (!writeQueue.isEmpty()) {
            ByteBuffer buf = writeQueue.peek();
            channel.write(buf);
            if (buf.hasRemaining()) {
                // Socket write buffer full, remain interested in OP_WRITE
                return;
            }
            writeQueue.poll();
        }

        // Write queue emptied, turn off OP_WRITE interest
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
    }

    public void close() {
        try {
            key.cancel();
            channel.close();
        } catch (IOException ignored) {
        }
    }
}
