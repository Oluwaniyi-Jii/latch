package com.latch.command;

import com.latch.protocol.Encoder;
import com.latch.storage.Database;

import java.util.List;

public class PingCommand implements Command {
    @Override
    public byte[] execute(List<String> args, Database db) {
        if (args.size() > 1) {
            return Encoder.encodeBulkString(args.get(1));
        }
        return Encoder.encodePong();
    }
}
