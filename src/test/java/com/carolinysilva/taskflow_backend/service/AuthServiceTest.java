package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.InvalidCredentialsException;
import com.carolinysilva.taskflow_backend.exception.RateLimitExceededException;
import com.carolinysilva.taskflow_backend.repository.UserRepository;
import com.carolinysilva.taskflow_backend.security.JwtService;
import com.carolinysilva.taskflow_backend.security.RateLimiter;
import com.carolinysilva.taskflow_backend.security.TokenRevocationList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private RateLimiter rateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter();
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, rateLimiter, new TokenRevocationList(), MAX_LOGIN_ATTEMPTS, 15);
    }

    @Test
    void login_shouldReturnTokenPair_whenCredentialsAreValid() {
        User user = new User("Carol", "carol@test.com", "hash");
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken("carol@test.com")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("carol@test.com")).thenReturn("refresh-token");

        AuthService.TokenPair result = authService.login("carol@test.com", "senha123", "127.0.0.1");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenPasswordIsWrong() {
        User user = new User("Carol", "carol@test.com", "hash");
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldThrowInvalidCredentials_whenUserDoesNotExist() {
        when(userRepository.findByEmail("does-not-exist@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("does-not-exist@test.com", "senha123", "127.0.0.1"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldBlockFurtherAttempts_afterReachingMaxFailedAttempts() {
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.empty());

        for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "127.0.0.1"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "127.0.0.1"))
                .isInstanceOf(RateLimitExceededException.class);
        verify(userRepository, org.mockito.Mockito.times(MAX_LOGIN_ATTEMPTS)).findByEmail("carol@test.com");
    }

    @Test
    void login_shouldTrackAttempts_perClientAndEmailIndependently() {
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.empty());

        for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "1.1.1.1"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "2.2.2.2"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldResetAttemptCounter_afterSuccessfulLogin() {
        User user = new User("Carol", "carol@test.com", "hash");
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);

        for (int i = 0; i < MAX_LOGIN_ATTEMPTS - 1; i++) {
            assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "127.0.0.1"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        authService.login("carol@test.com", "senha123", "127.0.0.1");

        for (int i = 0; i < MAX_LOGIN_ATTEMPTS; i++) {
            assertThatThrownBy(() -> authService.login("carol@test.com", "wrong", "127.0.0.1"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }
}
