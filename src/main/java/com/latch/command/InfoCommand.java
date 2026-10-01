package com.latch.command;

import com.latch.metrics.MetricsRegistry;
import com.latch.protocol.Encoder;
import com.latch.storage.Database;

import java.util.List;

public class InfoCommand implements Command {

    private final MetricsRegistry metrics;

    public InfoCommand(MetricsRegistry metrics) {
        this.metrics = metrics;
    }

    @Override
    public byte[] execute(List<String> args, Database db) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Server\r\n");
        sb.append("latch_version:1.0.0\r\n");
        sb.append("uptime_in_seconds:").append(metrics.getUptimeSeconds()).append("\r\n");

        sb.append("# Clients\r\n");
        sb.append("connected_clients:").append(metrics.getActiveClientsCount()).append("\r\n");

        sb.append("# Memory\r\n");
        sb.append("used_memory:").append(metrics.getUsedMemoryBytes()).append("\r\n");

        sb.append("# Stats\r\n");
        sb.append("total_commands_processed:").append(metrics.getTotalCommandsProcessed()).append("\r\n");
        sb.append("expired_keys:").append(metrics.getExpiredKeysCount()).append("\r\n");
        sb.append("db_keys:").append(db.size()).append("\r\n");

        return Encoder.encodeBulkString(sb.toString());
    }
}
