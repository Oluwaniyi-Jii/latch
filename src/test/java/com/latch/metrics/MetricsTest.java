package com.latch.metrics;

import com.latch.command.InfoCommand;
import com.latch.storage.ShardedDatabase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {

    @Test
    @DisplayName("Metrics & INFO command verification")
    void testMetricsAndInfoCommand() {
        MetricsRegistry metrics = new MetricsRegistry();
        metrics.incrementCommands();
        metrics.clientConnected();
        metrics.keyExpired();

        assertEquals(1, metrics.getTotalCommandsProcessed());
        assertEquals(1, metrics.getActiveClientsCount());
        assertEquals(1, metrics.getExpiredKeysCount());
        assertTrue(metrics.getUptimeSeconds() >= 0);

        ShardedDatabase db = new ShardedDatabase(4);
        InfoCommand infoCmd = new InfoCommand(metrics);
        byte[] infoResp = infoCmd.execute(List.of("INFO"), db);

        String infoText = new String(infoResp, StandardCharsets.UTF_8);
        assertTrue(infoText.contains("latch_version:1.0.0"));
        assertTrue(infoText.contains("total_commands_processed:1"));
    }
}
