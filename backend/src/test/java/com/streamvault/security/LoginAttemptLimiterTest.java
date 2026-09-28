package com.streamvault.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginAttemptLimiter Tests")
class LoginAttemptLimiterTest {

    /** Minimal mutable clock for moving time forward. */
    private static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    @DisplayName("Should block after max failures, per username + IP, case-insensitively")
    void blocksAfterMaxFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(new MutableClock());
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            assertFalse(limiter.isBlocked("Alice", "1.1.1.1"));
            limiter.recordFailure("Alice", "1.1.1.1");
        }

        assertTrue(limiter.isBlocked("alice", "1.1.1.1"));
        assertFalse(limiter.isBlocked("alice", "2.2.2.2"));
        assertFalse(limiter.isBlocked("bob", "1.1.1.1"));
    }

    @Test
    @DisplayName("Should unblock after the window and on reset")
    void unblocks() {
        MutableClock clock = new MutableClock();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("alice", "ip");
        }
        assertTrue(limiter.isBlocked("alice", "ip"));

        clock.now = clock.now.plus(LoginAttemptLimiter.WINDOW).plusSeconds(1);
        assertFalse(limiter.isBlocked("alice", "ip"));

        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("alice", "ip");
        }
        limiter.reset("alice", "ip");
        assertFalse(limiter.isBlocked("alice", "ip"));
    }
}
