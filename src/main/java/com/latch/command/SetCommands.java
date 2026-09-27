package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.SetValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SetCommands {

    public static class SAddCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'sadd' command");
            }
            String key = args.get(1);

            final int[] addedHolder = new int[1];
            db.compute(key, (k, currentVal) -> {
                Set<String> set;
                if (currentVal == null) {
                    set = new HashSet<>();
                } else if (currentVal instanceof SetValue s) {
                    set = s.value();
                } else {
                    throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
                }

                int added = 0;
                for (int i = 2; i < args.size(); i++) {
                    if (set.add(args.get(i))) {
                        added++;
                    }
                }
                addedHolder[0] = added;
                return new SetValue(set);
            });

            return Encoder.encodeInteger(addedHolder[0]);
        }
    }

    public static class SRemCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'srem' command");
            }
            String key = args.get(1);

            final int[] removedHolder = new int[1];
            db.compute(key, (k, currentVal) -> {
                if (currentVal == null) {
                    removedHolder[0] = 0;
                    return null;
                }
                if (currentVal instanceof SetValue s) {
                    Set<String> set = s.value();
                    int removed = 0;
                    for (int i = 2; i < args.size(); i++) {
                        if (set.remove(args.get(i))) {
                            removed++;
                        }
                    }
                    removedHolder[0] = removed;
                    return set.isEmpty() ? null : s;
                }
                throw new IllegalArgumentException("WRONGTYPE Operation against a key holding the wrong kind of value");
            });

            return Encoder.encodeInteger(removedHolder[0]);
        }
    }

    public static class SMembersCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'smembers' command");
            }
            String key = args.get(1);

            var val = db.get(key);
            if (val == null) {
                return Encoder.encodeArray(List.of());
            }
            if (!(val instanceof SetValue s)) {
                return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            List<byte[]> members = new ArrayList<>();
            for (String member : s.value()) {
                members.add(Encoder.encodeBulkString(member));
            }
            return Encoder.encodeArray(members);
        }
    }

    public static class SIsMemberCommand implements Command {
        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'sismember' command");
            }
            String key = args.get(1);
            String member = args.get(2);

            var val = db.get(key);
            if (val == null) {
                return Encoder.encodeInteger(0);
            }
            if (!(val instanceof SetValue s)) {
                return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
            }

            boolean isMember = s.value().contains(member);
            return Encoder.encodeInteger(isMember ? 1 : 0);
        }
    }
}
