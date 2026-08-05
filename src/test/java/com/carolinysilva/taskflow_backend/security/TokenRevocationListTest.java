package com.carolinysilva.taskflow_backend.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRevocationListTest {

    private final TokenRevocationList tokenRevocationList = new TokenRevocationList();

    @Test
    void isRevoked_shouldReturnFalse_forUnknownJti() {
        assertThat(tokenRevocationList.isRevoked("unknown-jti")).isFalse();
    }

    @Test
    void isRevoked_shouldReturnTrue_afterRevoking() {
        tokenRevocationList.revoke("jti-1", Instant.now().plus(1, ChronoUnit.HOURS));

        assertThat(tokenRevocationList.isRevoked("jti-1")).isTrue();
    }

    @Test
    void isRevoked_shouldReturnFalse_onceTheTokenWouldHaveNaturallyExpired() {
        tokenRevocationList.revoke("jti-1", Instant.now().minus(1, ChronoUnit.SECONDS));

        assertThat(tokenRevocationList.isRevoked("jti-1")).isFalse();
    }
}
