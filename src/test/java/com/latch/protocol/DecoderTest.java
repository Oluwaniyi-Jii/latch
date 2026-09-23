package com.latch.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    @Test
    @DisplayName("Decode Inline Command (PING\\r\\n and SET name alice\\r\\n)")
    void testDecodeInline() {
        ByteBuffer buf = ByteBuffer.wrap("PING\r\n".getBytes(StandardCharsets.UTF_8));
        List<String> args = Decoder.decode(buf);
        assertNotNull(args);
        assertEquals(List.of("PING"), args);

        ByteBuffer setBuf = ByteBuffer.wrap("SET name alice\r\n".getBytes(StandardCharsets.UTF_8));
        List<String> setArgs = Decoder.decode(setBuf);
        assertNotNull(setArgs);
        assertEquals(List.of("SET", "name", "alice"), setArgs);
    }

    @Test
    @DisplayName("Decode RESP Array Command (*3\\r\\n$3\\r\\nSET\\r\\n$4\\r\\nname\\r\\n$5\\r\\nalice\\r\n)")
    void testDecodeRespArray() {
        String raw = "*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$5\r\nalice\r\n";
        ByteBuffer buf = ByteBuffer.wrap(raw.getBytes(StandardCharsets.UTF_8));
        List<String> args = Decoder.decode(buf);

        assertNotNull(args);
        assertEquals(List.of("SET", "name", "alice"), args);
    }

    @Test
    @DisplayName("Decode Partial/Incomplete Frame returns null and restores buffer position")
    void testIncompleteFrame() {
        String partialRaw = "*3\r\n$3\r\nSET\r\n$4\r\nnam";
        ByteBuffer buf = ByteBuffer.wrap(partialRaw.getBytes(StandardCharsets.UTF_8));
        int initialPos = buf.position();

        List<String> args = Decoder.decode(buf);
        assertNull(args);
        assertEquals(initialPos, buf.position(), "Buffer position must be restored on incomplete decode");
    }
}
