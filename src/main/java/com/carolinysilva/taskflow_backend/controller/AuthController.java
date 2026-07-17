package com.carolinysilva.taskflow_backend.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolinysilva.taskflow_backend.dto.LoginRequest;
import com.carolinysilva.taskflow_backend.dto.LoginResponse;
import com.carolinysilva.taskflow_backend.dto.RefreshResponse;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
import com.carolinysilva.taskflow_backend.dto.UserResponse;
import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.service.AuthService;
import com.carolinysilva.taskflow_backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final UserService userService;
    private final AuthService authService;
    private final long refreshTokenExpirationMs;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public AuthController(
            UserService userService,
            AuthService authService,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${jwt.refresh-cookie-secure}") boolean refreshCookieSecure,
            @Value("${jwt.refresh-cookie-same-site}") String refreshCookieSameSite) {
        this.userService = userService;
        this.authService = authService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.TokenPair tokenPair = authService.login(request.email(), request.password());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken()).toString())
                .body(new LoginResponse(tokenPair.accessToken(), UserResponse.from(tokenPair.user())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthService.TokenPair tokenPair = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken()).toString())
                .body(new RefreshResponse(tokenPair.accessToken()));
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build();
    }
}
