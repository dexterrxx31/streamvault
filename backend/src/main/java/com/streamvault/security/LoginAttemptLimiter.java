package com.streamvault.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force guard for login: after {@link #MAX_FAILURES} failed
 * attempts for the same username + client IP within {@link #WINDOW}, further
 * attempts are rejected until the window expires. Single-instance only; a
 * multi-node deployment would need a shared store.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int PRUNE_THRESHOLD = 10_000;

    private record Attempts(int failures, Instant windowStart) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String username, String clientIp) {
        Attempts a = attempts.get(key(username, clientIp));
        return a != null && !expired(a) && a.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String username, String clientIp) {
        if (attempts.size() > PRUNE_THRESHOLD) {
            attempts.values().removeIf(this::expired);
        }
        attempts.merge(key(username, clientIp), new Attempts(1, clock.instant()),
                (old, fresh) -> expired(old) ? fresh : new Attempts(old.failures() + 1, old.windowStart()));
    }

    public void reset(String username, String clientIp) {
        attempts.remove(key(username, clientIp));
    }

    private boolean expired(Attempts a) {
        return a.windowStart().plus(WINDOW).isBefore(clock.instant());
    }

    private String key(String username, String clientIp) {
        return String.valueOf(username).toLowerCase(Locale.ROOT) + "|" + clientIp;
    }
}
