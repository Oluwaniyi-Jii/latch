package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;
import com.latch.storage.StringValue;

import java.util.List;

public class SetCommand implements Command {
    @Override
    public byte[] execute(List<String> args, Database db) {
        if (args.size() < 3) {
            return Encoder.encodeError("ERR wrong number of arguments for 'set' command");
        }
        String key = args.get(1);
        String value = args.get(2);

        db.set(key, new StringValue(value));
        return Encoder.encodeOk();
    }
}
