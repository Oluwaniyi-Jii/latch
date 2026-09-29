package com.latch.persistence;

import com.latch.command.CommandRegistry;
import com.latch.protocol.Decoder;
import com.latch.protocol.Encoder;
import com.latch.storage.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Point-in-time Snapshot Manager with Atomic File Rename.
 */
public class SnapshotManager {

    public static void saveSnapshot(Database db, Path snapshotPath) throws IOException {
        Path tempPath = Paths.get(snapshotPath.toString() + ".tmp");

        Map<String, Value> state = db.snapshot();

        try (FileChannel channel = FileChannel.open(tempPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            for (Map.Entry<String, Value> entry : state.entrySet()) {
                String key = entry.getKey();
                Value val = entry.getValue();

                List<String> commandArgs = convertValueToSetCommand(key, val);
                if (commandArgs == null) continue;

                List<byte[]> respElements = new ArrayList<>(commandArgs.size());
                for (String arg : commandArgs) {
                    respElements.add(Encoder.encodeBulkString(arg));
                }
                byte[] encoded = Encoder.encodeArray(respElements);

                ByteBuffer buf = ByteBuffer.wrap(encoded);
                while (buf.hasRemaining()) {
                    channel.write(buf);
                }
            }

            channel.force(true);
        }

        // Atomic File Rename
        Files.move(tempPath, snapshotPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public static int loadSnapshot(Database db, Path snapshotPath, CommandRegistry registry) throws IOException {
        if (!Files.exists(snapshotPath)) {
            return 0;
        }
        return AofReader.replayLog(snapshotPath, db, registry);
    }

    private static List<String> convertValueToSetCommand(String key, Value val) {
        if (val instanceof StringValue s) {
            return List.of("SET", key, s.value());
        } else if (val instanceof ListValue l) {
            List<String> args = new ArrayList<>();
            args.add("LPUSH");
            args.add(key);
            args.addAll(l.value());
            return args;
        } else if (val instanceof HashValue h) {
            List<String> args = new ArrayList<>();
            args.add("HSET");
            args.add(key);
            for (Map.Entry<String, String> e : h.value().entrySet()) {
                args.add(e.getKey());
                args.add(e.getValue());
            }
            return args;
        } else if (val instanceof SetValue s) {
            List<String> args = new ArrayList<>();
            args.add("SADD");
            args.add(key);
            args.addAll(s.value());
            return args;
        }
        return null;
    }
}
