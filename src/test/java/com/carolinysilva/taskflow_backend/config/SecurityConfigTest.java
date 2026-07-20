package com.carolinysilva.taskflow_backend.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig(List.of("http://localhost:5173")).passwordEncoder();

    @Test
    void encode_naoDeveArmazenarSenhaEmTextoPlano() {
        String hash = passwordEncoder.encode("minhaSenha123");

        assertThat(hash).isNotEqualTo("minhaSenha123");
        assertThat(hash).startsWith("$2");
    }

    @Test
    void matches_deveValidarSenhaCorreta() {
        String hash = passwordEncoder.encode("minhaSenha123");

        assertThat(passwordEncoder.matches("minhaSenha123", hash)).isTrue();
    }

    @Test
    void matches_deveRejeitarSenhaIncorreta() {
        String hash = passwordEncoder.encode("minhaSenha123");

        assertThat(passwordEncoder.matches("senhaErrada", hash)).isFalse();
    }
}
