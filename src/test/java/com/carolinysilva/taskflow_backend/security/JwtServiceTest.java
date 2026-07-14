package com.carolinysilva.taskflow_backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";
    private static final long ACCESS_EXPIRATION_MS = 15 * 60 * 1000L;
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private final JwtService jwtService = new JwtService(SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);

    @Test
    void generateAccessToken_deveGerarTokenValidoComSubjectCorreto() {
        String token = jwtService.generateAccessToken("carol@teste.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("carol@teste.com");
    }

    @Test
    void generateRefreshToken_deveGerarTokenValidoComSubjectCorreto() {
        String token = jwtService.generateRefreshToken("carol@teste.com");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractSubject(token)).isEqualTo("carol@teste.com");
    }

    @Test
    void isTokenValid_deveRetornarFalso_quandoTokenExpirado() {
        JwtService jwtServiceComExpiracaoCurta = new JwtService(SECRET, -1000L, -1000L);

        String token = jwtServiceComExpiracaoCurta.generateAccessToken("carol@teste.com");

        assertThat(jwtServiceComExpiracaoCurta.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_deveRetornarFalso_quandoTokenInvalido() {
        assertThat(jwtService.isTokenValid("token-completamente-invalido")).isFalse();
    }

    @Test
    void isTokenValid_deveRetornarFalso_quandoAssinaturaNaoConfere() {
        JwtService outroJwtService = new JwtService(
                "outro-segredo-completamente-diferente-e-com-32-chars", ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS);
        String token = outroJwtService.generateAccessToken("carol@teste.com");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
