package com.latch.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValueTest {

    @Test
    @DisplayName("Verify StringValue record creation and type name")
    void testStringValue() {
        StringValue stringVal = new StringValue("alice");
        assertEquals("alice", stringVal.value());
        assertEquals("string", stringVal.typeName());
        assertThrows(NullPointerException.class, () -> new StringValue(null));
    }

    @Test
    @DisplayName("Verify ListValue record creation and type name")
    void testListValue() {
        ListValue listVal = new ListValue();
        assertEquals("list", listVal.typeName());
        listVal.value().addFirst("item1");
        assertEquals("item1", listVal.value().peekFirst());
    }

    @Test
    @DisplayName("Verify HashValue record creation and type name")
    void testHashValue() {
        HashValue hashVal = new HashValue();
        assertEquals("hash", hashVal.typeName());
        hashVal.value().put("field1", "val1");
        assertEquals("val1", hashVal.value().get("field1"));
    }

    @Test
    @DisplayName("Verify SetValue record creation and type name")
    void testSetValue() {
        SetValue setVal = new SetValue();
        assertEquals("set", setVal.typeName());
        setVal.value().add("member1");
        assertTrue(setVal.value().contains("member1"));
    }

    @Test
    @DisplayName("Verify Java 25 sealed interface pattern matching switch exhaustiveness")
    void testSealedPatternMatching() {
        Value val = new StringValue("test");
        String result = switch (val) {
            case StringValue s -> "String: " + s.value();
            case ListValue l -> "List: " + l.value().size();
            case HashValue h -> "Hash: " + h.value().size();
            case SetValue s -> "Set: " + s.value().size();
        };
        assertEquals("String: test", result);
    }
}
