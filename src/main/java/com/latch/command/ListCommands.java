package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.ListValue;
import com.latch.storage.Value;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ListCommands {

    public static class LPushCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'lpush' command");
            }
            String key = args.get(1);

            Value resultVal = db.compute(key, (k, currentVal) -> {
                Deque<String> deque;
                if (currentVal == null) {
                    deque = new ArrayDeque<>();
                } else if (currentVal instanceof ListValue l) {
                    deque = l.value();
                } else {
                    throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }

                for (int i = 2; i < args.size(); i++) {
                    deque.addFirst(args.get(i));
                }
                return new ListValue(deque);
            });

            int finalSize = ((ListValue) resultVal).value().size();

            return Encoder.encodeInteger(finalSize);
        }
    }

    public static class LPopCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'lpop' command");
            }
            String key = args.get(1);

            final String[] popped = new String[1];
            db.compute(key, (k, currentVal) -> {
                if (currentVal == null) {
                    popped[0] = null;
                    return null;
                }
                if (currentVal instanceof ListValue l) {
                    Deque<String> deque = l.value();
                    popped[0] = deque.pollFirst();
                    return deque.isEmpty() ? null : l;
                }
                throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
            });

            return Encoder.encodeBulkString(popped[0]);
        }
    }

    public static class LRangeCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 4) {
                return Encoder.encodeError("ERR wrong number of arguments for 'lrange' command");
            }
            String key = args.get(1);
            int start;
            int stop;
            try {
                start = Integer.parseInt(args.get(2));
                stop = Integer.parseInt(args.get(3));
            } catch (NumberFormatException e) {
                return Encoder.encodeError("ERR value is not an integer or out of range");
            }

            var val = db.get(key);
            if (val == null) {
                return Encoder.encodeArray(List.of());
            }
            if (!(val instanceof ListValue l)) {
                return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            List<String> elements = new ArrayList<>(l.value());
            int size = elements.size();

            if (start < 0) start = size + start;
            if (stop < 0) stop = size + stop;

            if (start < 0) start = 0;
            if (stop >= size) stop = size - 1;

            if (start > stop || start >= size) {
                return Encoder.encodeArray(List.of());
            }

            List<byte[]> respElements = new ArrayList<>();
            for (int i = start; i <= stop; i++) {
                respElements.add(Encoder.encodeBulkString(elements.get(i)));
            }
            return Encoder.encodeArray(respElements);
        }
    }
}
