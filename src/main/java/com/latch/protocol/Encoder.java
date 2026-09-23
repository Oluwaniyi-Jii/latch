package com.latch.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RESP Protocol Encoder.
 * Serializes Java objects and primitives into Redis Serialization Protocol (RESP2) formatted byte arrays/buffers.
 */
public class Encoder {

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_BULK_STRING = "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NULL_ARRAY = "*-1\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OK_SIMPLE_STRING = "+OK\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PONG_SIMPLE_STRING = "+PONG\r\n".getBytes(StandardCharsets.UTF_8);

    public static byte[] encodeSimpleString(String content) {
        return ("+" + content + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeOk() {
        return OK_SIMPLE_STRING;
    }

    public static byte[] encodePong() {
        return PONG_SIMPLE_STRING;
    }

    public static byte[] encodeError(String errorMessage) {
        return ("-" + errorMessage + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeInteger(long value) {
        return (":" + value + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] encodeBulkString(String content) {
        if (content == null) {
            return NULL_BULK_STRING;
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String header = "$" + bytes.length + "\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] result = new byte[headerBytes.length + bytes.length + 2];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(bytes, 0, result, headerBytes.length, bytes.length);
        System.arraycopy(CRLF, 0, result, headerBytes.length + bytes.length, 2);
        return result;
    }

    public static byte[] encodeNullBulkString() {
        return NULL_BULK_STRING;
    }

    public static byte[] encodeArray(List<byte[]> elements) {
        if (elements == null) {
            return NULL_ARRAY;
        }
        String header = "*" + elements.size() + "\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        int totalLen = headerBytes.length;
        for (byte[] elem : elements) {
            totalLen += elem.length;
        }

        byte[] result = new byte[totalLen];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);

        int offset = headerBytes.length;
        for (byte[] elem : elements) {
            System.arraycopy(elem, 0, result, offset, elem.length);
            offset += elem.length;
        }
        return result;
    }

    public static byte[] encodeNullArray() {
        return NULL_ARRAY;
    }
}
