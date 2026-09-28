package com.latch.persistence;

import com.latch.protocol.Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Append-Only File (AOF) Log Writer using Java NIO FileChannel.
 */
public class AofWriter implements AutoCloseable {

    private final Path aofPath;
    private final FsyncPolicy fsyncPolicy;
    private FileChannel fileChannel;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final ScheduledExecutorService syncScheduler;

    public AofWriter(Path aofPath, FsyncPolicy fsyncPolicy) throws IOException {
        this.aofPath = aofPath;
        this.fsyncPolicy = fsyncPolicy;
        this.fileChannel = FileChannel.open(aofPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND);

        if (fsyncPolicy == FsyncPolicy.EVERYSEC) {
            this.syncScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Latch-AOF-FsyncWorker");
                t.setDaemon(true);
                return t;
            });
            this.syncScheduler.scheduleWithFixedDelay(this::forceFsync, 1, 1, TimeUnit.SECONDS);
        } else {
            this.syncScheduler = null;
        }
    }

    public void append(List<String> commandArgs) throws IOException {
        if (commandArgs == null || commandArgs.isEmpty()) {
            return;
        }

        List<byte[]> respElements = new ArrayList<>(commandArgs.size());
        for (String arg : commandArgs) {
            respElements.add(Encoder.encodeBulkString(arg));
        }
        byte[] encodedArray = Encoder.encodeArray(respElements);
        ByteBuffer buf = ByteBuffer.wrap(encodedArray);

        writeLock.lock();
        try {
            while (buf.hasRemaining()) {
                fileChannel.write(buf);
            }

            if (fsyncPolicy == FsyncPolicy.ALWAYS) {
                fileChannel.force(false);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void forceFsync() {
        writeLock.lock();
        try {
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.force(false);
            }
        } catch (IOException ignored) {
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        if (syncScheduler != null) {
            syncScheduler.shutdownNow();
        }
        writeLock.lock();
        try {
            if (fileChannel != null && fileChannel.isOpen()) {
                fileChannel.force(true);
                fileChannel.close();
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Path getAofPath() {
        return aofPath;
    }
}
