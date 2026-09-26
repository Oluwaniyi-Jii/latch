package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;

import java.util.List;

public class DelCommand implements Command {
    @Override
    public byte[] execute(List<String> args, Database db) {
        if (args.size() < 2) {
            return Encoder.encodeError("ERR wrong number of arguments for 'del' command");
        }
        List<String> keys = args.subList(1, args.size());
        int deleted = db.del(keys);
        return Encoder.encodeInteger(deleted);
    }
}
