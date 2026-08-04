package com.carolinysilva.taskflow_backend.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void isBlocked_shouldReturnFalse_whenKeyHasNoAttempts() {
        assertThat(rateLimiter.isBlocked("key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void isBlocked_shouldReturnFalse_whenAttemptsAreBelowLimit() {
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));

        assertThat(rateLimiter.isBlocked("key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void isBlocked_shouldReturnTrue_whenAttemptsReachTheLimit() {
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));

        assertThat(rateLimiter.isBlocked("key", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void isBlocked_shouldReturnFalse_whenWindowHasExpired() {
        rateLimiter.recordAttempt("key", Duration.ofMillis(-1));

        assertThat(rateLimiter.isBlocked("key", 1, Duration.ofMillis(-1))).isFalse();
    }

    @Test
    void reset_shouldClearAttemptsForKey() {
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));
        rateLimiter.recordAttempt("key", Duration.ofMinutes(1));

        rateLimiter.reset("key");

        assertThat(rateLimiter.isBlocked("key", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void attempts_shouldBeTrackedIndependently_perKey() {
        rateLimiter.recordAttempt("key-a", Duration.ofMinutes(1));
        rateLimiter.recordAttempt("key-a", Duration.ofMinutes(1));

        assertThat(rateLimiter.isBlocked("key-a", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(rateLimiter.isBlocked("key-b", 2, Duration.ofMinutes(1))).isFalse();
    }
}
