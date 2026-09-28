package com.latch.command;

import com.latch.expiration.ExpirationManager;
import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.StringValue;

import java.util.List;

public class TtlCommands {

    public static class ExpireCommand implements Command {
        private final ExpirationManager expirationManager;

        public ExpireCommand(ExpirationManager expirationManager) {
            this.expirationManager = expirationManager;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 3) {
                return Encoder.encodeError("ERR wrong number of arguments for 'expire' command");
            }
            String key = args.get(1);
            long seconds;
            try {
                seconds = Long.parseLong(args.get(2));
            } catch (NumberFormatException e) {
                return Encoder.encodeError("ERR value is not an integer or out of range");
            }

            if (!db.exists(key)) {
                return Encoder.encodeInteger(0);
            }

            expirationManager.setExpirySeconds(key, seconds);
            return Encoder.encodeInteger(1);
        }
    }

    public static class TtlCommand implements Command {
        private final ExpirationManager expirationManager;

        public TtlCommand(ExpirationManager expirationManager) {
            this.expirationManager = expirationManager;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'ttl' command");
            }
            String key = args.get(1);
            long ttl = expirationManager.getTtlSeconds(key);
            return Encoder.encodeInteger(ttl);
        }
    }

    public static class PersistCommand implements Command {
        private final ExpirationManager expirationManager;

        public PersistCommand(ExpirationManager expirationManager) {
            this.expirationManager = expirationManager;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'persist' command");
            }
            String key = args.get(1);
            if (!db.exists(key)) {
                return Encoder.encodeInteger(0);
            }
            boolean removed = expirationManager.persist(key);
            return Encoder.encodeInteger(removed ? 1 : 0);
        }
    }

    public static class SetExCommand implements Command {
        private final ExpirationManager expirationManager;

        public SetExCommand(ExpirationManager expirationManager) {
            this.expirationManager = expirationManager;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 4) {
                return Encoder.encodeError("ERR wrong number of arguments for 'setex' command");
            }
            String key = args.get(1);
            long seconds;
            try {
                seconds = Long.parseLong(args.get(2));
            } catch (NumberFormatException e) {
                return Encoder.encodeError("ERR value is not an integer or out of range");
            }
            String value = args.get(3);

            db.set(key, new StringValue(value));
            expirationManager.setExpirySeconds(key, seconds);
            return Encoder.encodeOk();
        }
    }
}
