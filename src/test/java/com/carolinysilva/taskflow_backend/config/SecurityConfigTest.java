package com.carolinysilva.taskflow_backend.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig(List.of("http://localhost:5173")).passwordEncoder();

    @Test
    void encode_shouldNotStorePasswordInPlainText() {
        String hash = passwordEncoder.encode("myPassword123");

        assertThat(hash).isNotEqualTo("myPassword123");
        assertThat(hash).startsWith("$2");
    }

    @Test
    void matches_shouldValidateCorrectPassword() {
        String hash = passwordEncoder.encode("myPassword123");

        assertThat(passwordEncoder.matches("myPassword123", hash)).isTrue();
    }

    @Test
    void matches_shouldRejectIncorrectPassword() {
        String hash = passwordEncoder.encode("myPassword123");

        assertThat(passwordEncoder.matches("wrongPassword", hash)).isFalse();
    }
}
