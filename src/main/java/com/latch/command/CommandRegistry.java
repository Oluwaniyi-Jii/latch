package com.latch.command;

import com.latch.expiration.ExpirationManager;
import com.latch.protocol.Encoder;
import com.latch.storage.Database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        registerDefaults();
    }

    public CommandRegistry(ExpirationManager expirationManager) {
        registerDefaults();
        if (expirationManager != null) {
            registerTtlCommands(expirationManager);
        }
    }

    public void register(String name, Command command) {
        commands.put(name.toUpperCase(), command);
    }

    public void registerTtlCommands(ExpirationManager expirationManager) {
        register("EXPIRE", new TtlCommands.ExpireCommand(expirationManager));
        register("TTL", new TtlCommands.TtlCommand(expirationManager));
        register("PERSIST", new TtlCommands.PersistCommand(expirationManager));
        register("SETEX", new TtlCommands.SetExCommand(expirationManager));
    }

    private void registerDefaults() {
        // Core Commands
        register("PING", new PingCommand());
        register("SET", new SetCommand());
        register("GET", new GetCommand());
        register("DEL", new DelCommand());
        register("EXISTS", new ExistsCommand());

        // List Commands
        register("LPUSH", new ListCommands.LPushCommand());
        register("LPOP", new ListCommands.LPopCommand());
        register("LRANGE", new ListCommands.LRangeCommand());

        // Hash Commands
        register("HSET", new HashCommands.HSetCommand());
        register("HGET", new HashCommands.HGetCommand());
        register("HDEL", new HashCommands.HDelCommand());
        register("HGETALL", new HashCommands.HGetAllCommand());

        // Set Commands
        register("SADD", new SetCommands.SAddCommand());
        register("SREM", new SetCommands.SRemCommand());
        register("SMEMBERS", new SetCommands.SMembersCommand());
        register("SISMEMBER", new SetCommands.SIsMemberCommand());
    }

    public byte[] execute(List<String> args, Database db) {
        if (args == null || args.isEmpty()) {
            return Encoder.encodeError("ERR empty command");
        }
        String cmdName = args.get(0).toUpperCase();
        Command cmd = commands.get(cmdName);

        if (cmd == null) {
            return Encoder.encodeError("ERR unknown command '" + cmdName + "'");
        }
        try {
            return cmd.execute(args, db);
        } catch (IllegalArgumentException e) {
            return Encoder.encodeError(e.getMessage());
        }
    }
}
