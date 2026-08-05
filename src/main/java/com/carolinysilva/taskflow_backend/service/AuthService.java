package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.InvalidCredentialsException;
import com.carolinysilva.taskflow_backend.exception.InvalidTokenException;
import com.carolinysilva.taskflow_backend.exception.RateLimitExceededException;
import com.carolinysilva.taskflow_backend.repository.UserRepository;
import com.carolinysilva.taskflow_backend.security.JwtService;
import com.carolinysilva.taskflow_backend.security.RateLimiter;
import com.carolinysilva.taskflow_backend.security.TokenRevocationList;
import com.carolinysilva.taskflow_backend.util.EmailNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final TokenRevocationList tokenRevocationList;
    private final int maxLoginAttempts;
    private final Duration loginAttemptWindow;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RateLimiter rateLimiter,
            TokenRevocationList tokenRevocationList,
            @Value("${rate-limit.login.max-attempts}") int maxLoginAttempts,
            @Value("${rate-limit.login.window-minutes}") long loginAttemptWindowMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.tokenRevocationList = tokenRevocationList;
        this.maxLoginAttempts = maxLoginAttempts;
        this.loginAttemptWindow = Duration.ofMinutes(loginAttemptWindowMinutes);
    }

    public TokenPair login(String email, String rawPassword, String clientKey) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        String rateLimitKey = "login:" + clientKey + ":" + normalizedEmail;

        if (rateLimiter.isBlocked(rateLimitKey, maxLoginAttempts, loginAttemptWindow)) {
            throw new RateLimitExceededException(
                    "TOO_MANY_ATTEMPTS", "Muitas tentativas de login. Tente novamente em alguns minutos");
        }

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            rateLimiter.recordAttempt(rateLimitKey, loginAttemptWindow);
            throw new InvalidCredentialsException("INVALID_CREDENTIALS", "E-mail ou senha inválidos");
        }

        rateLimiter.reset(rateLimitKey);

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new TokenPair(accessToken, refreshToken, user);
    }

    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtService.isTokenValid(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)
                || tokenRevocationList.isRevoked(jwtService.extractId(refreshToken))) {
            throw new InvalidTokenException("INVALID_REFRESH_TOKEN", "Refresh token inválido ou expirado");
        }

        String email = jwtService.extractSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("INVALID_REFRESH_TOKEN", "Refresh token inválido ou expirado"));

        revokeToken(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new TokenPair(newAccessToken, newRefreshToken, user);
    }

    public void logout(String accessToken, String refreshToken) {
        revokeToken(accessToken);
        revokeToken(refreshToken);
    }

    private void revokeToken(String token) {
        if (token == null || token.isBlank() || !jwtService.isTokenValid(token)) {
            return;
        }
        tokenRevocationList.revoke(jwtService.extractId(token), jwtService.extractExpiration(token));
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {
    }
}
