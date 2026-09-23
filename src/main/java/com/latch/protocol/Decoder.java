package com.latch.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RESP Protocol Decoder.
 * Decodes incoming byte buffers into parsed command arguments (List of Strings).
 * Supports both standard RESP Arrays (*N\r\n...) and inline text format (PING\r\n).
 */
public class Decoder {

    /**
     * Decodes a frame from the provided ByteBuffer.
     * Returns a List of String arguments if a complete frame is available, or null if incomplete.
     */
    public static List<String> decode(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            return null;
        }

        buffer.mark();
        byte firstByte = buffer.get();

        if (firstByte == '*') {
            // RESP Array parsing
            List<String> result = parseArray(buffer);
            if (result == null) {
                buffer.reset();
            }
            return result;
        } else {
            // Inline text parsing (e.g., PING\r\n or SET k v\r\n)
            buffer.reset();
            List<String> result = parseInline(buffer);
            if (result == null) {
                buffer.reset();
            }
            return result;
        }
    }

    private static List<String> parseArray(ByteBuffer buffer) {
        String countStr = readLine(buffer);
        if (countStr == null) {
            return null;
        }

        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return null;
        }

        if (count <= 0) {
            return new ArrayList<>();
        }

        List<String> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (!buffer.hasRemaining()) {
                return null;
            }

            byte typeByte = buffer.get();
            if (typeByte != '$') {
                return null; // expected bulk string prefix
            }

            String lenStr = readLine(buffer);
            if (lenStr == null) {
                return null;
            }

            int len;
            try {
                len = Integer.parseInt(lenStr);
            } catch (NumberFormatException e) {
                return null;
            }

            if (len < 0) {
                args.add(null);
                continue;
            }

            if (buffer.remaining() < len + 2) { // data + \r\n
                return null;
            }

            byte[] data = new byte[len];
            buffer.get(data);

            // Read trailing \r\n
            if (buffer.get() != '\r' || buffer.get() != '\n') {
                return null;
            }

            args.add(new String(data, StandardCharsets.UTF_8));
        }

        return args;
    }

    private static List<String> parseInline(ByteBuffer buffer) {
        String line = readLine(buffer);
        if (line == null) {
            return null;
        }

        String[] parts = line.trim().split("\\s+");
        List<String> args = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) {
                args.add(p);
            }
        }
        return args;
    }

    private static String readLine(ByteBuffer buffer) {
        int startPos = buffer.position();
        boolean foundCr = false;

        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == '\r') {
                foundCr = true;
            } else if (foundCr && b == '\n') {
                int endPos = buffer.position() - 2;
                int len = endPos - startPos;
                byte[] lineBytes = new byte[len];

                int savedPos = buffer.position();
                buffer.position(startPos);
                buffer.get(lineBytes);
                buffer.position(savedPos);

                return new String(lineBytes, StandardCharsets.UTF_8);
            } else {
                foundCr = false;
            }
        }

        // Incomplete line
        buffer.position(startPos);
        return null;
    }
}
