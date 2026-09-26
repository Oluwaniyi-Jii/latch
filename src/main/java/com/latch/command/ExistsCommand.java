package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;

import java.util.List;

public class ExistsCommand implements Command {
    @Override
    public byte[] execute(List<String> args, Database db) {
        if (args.size() < 2) {
            return Encoder.encodeError("ERR wrong number of arguments for 'exists' command");
        }
        int count = 0;
        for (int i = 1; i < args.size(); i++) {
            if (db.exists(args.get(i))) {
                count++;
            }
        }
        return Encoder.encodeInteger(count);
    }
}
