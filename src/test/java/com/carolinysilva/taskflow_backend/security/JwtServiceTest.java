package com.carolinysilva.taskflow_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";
    private static final long ACCESS_EXPIRATION_MS = 15 * 60 * 1000L;
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final JwtService jwtService = new JwtService(SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);

    @Test
    void generateAccessToken_shouldGenerateValidTokenWithCorrectSubject() {
        String token = jwtService.generateAccessToken("carol@test.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("carol@test.com");
    }

    @Test
    void generateRefreshToken_shouldGenerateValidTokenWithCorrectSubject() {
        String token = jwtService.generateRefreshToken("carol@test.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("carol@test.com");
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        JwtService jwtServiceWithShortExpiration = new JwtService(SECRET, -1000L, -1000L);

        String token = jwtServiceWithShortExpiration.generateAccessToken("carol@test.com");

        assertThat(jwtServiceWithShortExpiration.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("completely-invalid-token")).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenSignatureDoesNotMatch() {
        JwtService anotherJwtService = new JwtService(
                "another-completely-different-secret-with-32-chars", ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);
        String token = anotherJwtService.generateAccessToken("carol@test.com");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
