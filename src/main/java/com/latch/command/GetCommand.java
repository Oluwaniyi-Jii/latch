package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.StringValue;
import com.latch.storage.Value;

import java.util.List;

public class GetCommand implements Command {
    @Override
    public byte[] execute(List<String> args, Database db) {
        if (args.size() < 2) {
            return Encoder.encodeError("ERR wrong number of arguments for 'get' command");
        }
        String key = args.get(1);
        Value val = db.get(key);

        if (val == null) {
            return Encoder.encodeNullBulkString();
        }

        if (val instanceof StringValue s) {
            return Encoder.encodeBulkString(s.value());
        }

        return Encoder.encodeError("WRONGTYPE Operation against a key holding the wrong kind of value");
    }
}
