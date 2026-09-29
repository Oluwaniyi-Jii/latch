package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.transaction.TransactionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxCommands {

    public static class TransactionState {
        private boolean inMulti = false;
        private final List<List<String>> queuedCommands = new ArrayList<>();
        private final Map<String, Long> watchedKeys = new HashMap<>();

        public boolean isInMulti() {
            return inMulti;
        }

        public void setInMulti(boolean inMulti) {
            this.inMulti = inMulti;
        }

        public List<List<String>> getQueuedCommands() {
            return queuedCommands;
        }

        public Map<String, Long> getWatchedKeys() {
            return watchedKeys;
        }

        public void reset() {
            this.inMulti = false;
            this.queuedCommands.clear();
            this.watchedKeys.clear();
        }
    }

    public static class MultiCommand implements Command {
        private final TransactionState txState;

        public MultiCommand(TransactionState txState) {
            this.txState = txState;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (txState.isInMulti()) {
                return Encoder.encodeError("ERR MULTI calls cannot be nested");
            }
            txState.setInMulti(true);
            return Encoder.encodeOk();
        }
    }

    public static class DiscardCommand implements Command {
        private final TransactionState txState;

        public DiscardCommand(TransactionState txState) {
            this.txState = txState;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (!txState.isInMulti()) {
                return Encoder.encodeError("ERR DISCARD without MULTI");
            }
            txState.reset();
            return Encoder.encodeOk();
        }
    }

    public static class WatchCommand implements Command {
        private final TransactionState txState;
        private final TransactionManager txManager;

        public WatchCommand(TransactionState txState, TransactionManager txManager) {
            this.txState = txState;
            this.txManager = txManager;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (args.size() < 2) {
                return Encoder.encodeError("ERR wrong number of arguments for 'watch' command");
            }
            if (txState.isInMulti()) {
                return Encoder.encodeError("ERR WATCH inside MULTI is not allowed");
            }

            for (int i = 1; i < args.size(); i++) {
                String key = args.get(i);
                long currentVersion = txManager.getKeyVersion(key);
                txState.getWatchedKeys().put(key, currentVersion);
            }
            return Encoder.encodeOk();
        }
    }

    public static class UnwatchCommand implements Command {
        private final TransactionState txState;

        public UnwatchCommand(TransactionState txState) {
            this.txState = txState;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            txState.getWatchedKeys().clear();
            return Encoder.encodeOk();
        }
    }

    public static class ExecCommand implements Command {
        private final TransactionState txState;
        private final TransactionManager txManager;
        private final CommandRegistry registry;

        public ExecCommand(TransactionState txState, TransactionManager txManager, CommandRegistry registry) {
            this.txState = txState;
            this.txManager = txManager;
            this.registry = registry;
        }

        @Override
        public byte[] execute(List<String> args, Database db) {
            if (!txState.isInMulti()) {
                return Encoder.encodeError("ERR EXEC without MULTI");
            }

            // Verify watched keys optimistic concurrency control
            if (txManager.isWatchedKeyModified(txState.getWatchedKeys())) {
                txState.reset();
                return Encoder.encodeNullArray(); // Abort transaction
            }

            List<List<String>> queued = new ArrayList<>(txState.getQueuedCommands());
            txState.reset();

            List<byte[]> results = new ArrayList<>();
            for (List<String> cmdArgs : queued) {
                byte[] resp = registry.execute(cmdArgs, db);
                results.add(resp);

                // Increment version if command mutates a key
                if (cmdArgs.size() > 1 && isMutatingCommand(cmdArgs.get(0))) {
                    txManager.incrementVersion(cmdArgs.get(1));
                }
            }

            return Encoder.encodeArray(results);
        }

        private boolean isMutatingCommand(String cmd) {
            String upper = cmd.toUpperCase();
            return upper.equals("SET") || upper.equals("DEL") || upper.equals("LPUSH") || upper.equals("LPOP")
                    || upper.equals("HSET") || upper.equals("HDEL") || upper.equals("SADD") || upper.equals("SREM") || upper.equals("SETEX");
        }
    }
}
