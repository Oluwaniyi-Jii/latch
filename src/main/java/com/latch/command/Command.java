package com.latch.command;

import com.latch.storage.Database;

import java.util.List;

/**
 * Command interface for executing database operations.
 */
@FunctionalInterface
public interface Command {
    /**
     * Executes command with given string arguments on the database.
     * Returns RESP encoded byte array response.
     */
    byte[] execute(List<String> args, Database db);
}
