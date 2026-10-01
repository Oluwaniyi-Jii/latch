package com.latch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LatchServerTest {

    @Test
    @DisplayName("Smoke test to verify JUnit 5 configuration")
    void testBootstrap() {
        assertTrue(true, "Latch server environment bootstrapped successfully");
    }
}
