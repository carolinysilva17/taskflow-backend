package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.InvalidCredentialsException;
import com.carolinysilva.taskflow_backend.exception.InvalidTokenException;
import com.carolinysilva.taskflow_backend.repository.UserRepository;
import com.carolinysilva.taskflow_backend.security.JwtService;
import com.carolinysilva.taskflow_backend.util.EmailNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenPair login(String email, String rawPassword) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("E-mail ou senha inválidos"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("E-mail ou senha inválidos");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new TokenPair(accessToken, refreshToken, user);
    }

    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtService.isTokenValid(refreshToken)
                || !jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Refresh token inválido ou expirado");
        }

        String email = jwtService.extractSubject(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido ou expirado"));

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());
        return new TokenPair(newAccessToken, newRefreshToken, user);
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {
    }
}
