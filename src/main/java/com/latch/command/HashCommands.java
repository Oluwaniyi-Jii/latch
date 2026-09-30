package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.HashValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashCommands {

    public static class HSetCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 4 || (args.size() % 2 != 0)) {
                return Encoder.encodeError("ERR wrong number of arguments for 'hset' command");
            }
            String key = args.get(1);

            int addedCount = 0;
            final int[] addedHolder = new int[1];

            db.compute(key, (k, currentVal) -> {
                Map<String, String> map;
                if (currentVal == null) {
                    map = new HashMap<>();
                } else if (currentVal instanceof HashValue h) {
                    map = h.value();
                } else {
                    throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }

                int newFields = 0;
                for (int i = 2; i < args.size(); i += 2) {
                    String field = args.get(i);
                    String value = args.get(i + 1);
                    if (map.put(field, value) == null) {
                        newFields++;
                    }
                }
                addedHolder[0] = newFields;
                return new HashValue(map);
            });

            return Encoder.encodeInteger(addedHolder[0]);
        }
    }

    public static class HGetCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'hget' command");
            }
            String key = args.get(1);
            String field = args.get(2);

            var val = db.get(key);
            if (val == null) {
                return Encoder.encodeNullBulkString();
            }
            if (!(val instanceof HashValue h)) {
                return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            String fieldVal = h.value().get(field);
            return Encoder.encodeBulkString(fieldVal);
        }
    }

    public static class HDelCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'hdel' command");
            }
            String key = args.get(1);

            final int[] deletedHolder = new int[1];
            db.compute(key, (k, currentVal) -> {
                if (currentVal == null) {
                    deletedHolder[0] = 0;
                    return null;
                }
                if (currentVal instanceof HashValue h) {
                    Map<String, String> map = h.value();
                    int deleted = 0;
                    for (int i = 2; i < args.size(); i++) {
                        if (map.remove(args.get(i)) != null) {
                            deleted++;
                        }
                    }
                    deletedHolder[0] = deleted;
                    return map.isEmpty() ? null : h;
                }
                throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
            });

            return Encoder.encodeInteger(deletedHolder[0]);
        }
    }

    public static class HGetAllCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'hgetall' command");
            }
            String key = args.get(1);

            var val = db.get(key);
            if (val == null) {
                return Encoder.encodeArray(List.of());
            }
            if (!(val instanceof HashValue h)) {
                return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            List<byte[]> pairs = new ArrayList<>();
            for (Map.Entry<String, String> entry : h.value().entrySet()) {
                pairs.add(Encoder.encodeBulkString(entry.getKey()));
                pairs.add(Encoder.encodeBulkString(entry.getValue()));
            }
            return Encoder.encodeArray(pairs);
        }
    }
}
