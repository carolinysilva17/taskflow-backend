package com.carolinysilva.taskflow_backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isBlocked(String key, int maxAttempts, Duration window) {
        Attempt attempt = attemptsByKey.get(key);
        if (attempt == null || isExpired(attempt, window)) {
            return false;
        }
        return attempt.count.get() >= maxAttempts;
    }

    public void recordAttempt(String key, Duration window) {
        attemptsByKey.compute(key, (ignoredKey, existing) -> {
            if (existing == null || isExpired(existing, window)) {
                return new Attempt();
            }
            existing.count.incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        attemptsByKey.remove(key);
    }

    private boolean isExpired(Attempt attempt, Duration window) {
        return Duration.between(attempt.windowStart, Instant.now()).compareTo(window) > 0;
    }

    private static final class Attempt {
        private final Instant windowStart = Instant.now();
        private final AtomicInteger count = new AtomicInteger(1);
    }
}
