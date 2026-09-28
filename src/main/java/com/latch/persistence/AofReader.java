package com.latch.persistence;

import com.latch.command.CommandRegistry;
import com.latch.protocol.Decoder;
import com.latch.storage.Database;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * AOF Reader & Log Replay Engine for Database Crash Recovery.
 */
public class AofReader {

    public static int replayLog(Path aofPath, Database db, CommandRegistry registry) throws IOException {
        if (!Files.exists(aofPath)) {
            return 0;
        }

        int replayedCount = 0;
        try (FileChannel channel = FileChannel.open(aofPath, StandardOpenOption.READ)) {
            long fileSize = channel.size();
            if (fileSize == 0) {
                return 0;
            }

            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(fileSize, 64 * 1024)); // 64KB chunk
            long readPosition = 0;

            while (readPosition < fileSize) {
                buffer.clear();
                int bytesRead = channel.read(buffer, readPosition);
                if (bytesRead == -1) {
                    break;
                }
                buffer.flip();

                while (buffer.hasRemaining()) {
                    int posBefore = buffer.position();
                    List<String> commandArgs = Decoder.decode(buffer);
                    if (commandArgs == null) {
                        // Partial frame in buffer, adjust readPosition to posBefore
                        buffer.position(posBefore);
                        break;
                    }

                    registry.execute(commandArgs, db);
                    replayedCount++;
                }

                readPosition += buffer.position();
            }
        }

        return replayedCount;
    }
}
