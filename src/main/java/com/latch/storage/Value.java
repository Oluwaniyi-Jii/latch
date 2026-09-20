package com.latch.storage;

/**
 * Sealed interface representing all supported data values in Latch.
 * Sealed interfaces in Java 21+ enforce compile-time pattern matching exhaustiveness.
 */
public sealed interface Value permits StringValue, ListValue, HashValue, SetValue {
    /**
     * Returns a human-readable type identifier (e.g. "string", "list", "hash", "set").
     */
    String typeName();
}
