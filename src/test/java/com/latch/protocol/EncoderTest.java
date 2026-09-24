package com.latch.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderTest {

    @Test
    @DisplayName("Encode Simple String (+PONG\\r\\n and +OK\\r\\n)")
    void testEncodeSimpleString() {
        assertEquals("+PONG\r\n", new String(Encoder.encodePong(), StandardCharsets.UTF_8));
        assertEquals("+OK\r\n", new String(Encoder.encodeOk(), StandardCharsets.UTF_8));
        assertEquals("+CUSTOM\r\n", new String(Encoder.encodeSimpleString("CUSTOM"), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode Error (-ERR message\\r\\n)")
    void testEncodeError() {
        assertEquals("-ERR unknown command\r\n", new String(Encoder.encodeError("ERR unknown command"), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode Integer (:100\\r\\n)")
    void testEncodeInteger() {
        assertEquals(":100\r\n", new String(Encoder.encodeInteger(100), StandardCharsets.UTF_8));
        assertEquals(":-5\r\n", new String(Encoder.encodeInteger(-5), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode Bulk String ($5\\r\\nalice\\r\\n and $-1\\r\\n)")
    void testEncodeBulkString() {
        assertEquals("$5\r\nalice\r\n", new String(Encoder.encodeBulkString("alice"), StandardCharsets.UTF_8));
        assertEquals("$-1\r\n", new String(Encoder.encodeBulkString(null), StandardCharsets.UTF_8));
        assertEquals("$-1\r\n", new String(Encoder.encodeNullBulkString(), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Encode RESP Array (*2\\r\\n$3\\r\\nfoo\\r\\n$3\\r\\nbar\\r\\n)")
    void testEncodeArray() {
        byte[] elem1 = Encoder.encodeBulkString("foo");
        byte[] elem2 = Encoder.encodeBulkString("bar");
        byte[] array = Encoder.encodeArray(List.of(elem1, elem2));

        assertEquals("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n", new String(array, StandardCharsets.UTF_8));
        assertEquals("*-1\r\n", new String(Encoder.encodeNullArray(), StandardCharsets.UTF_8));
    }
}
