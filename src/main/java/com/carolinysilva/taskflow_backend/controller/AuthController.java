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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolinysilva.taskflow_backend.dto.LoginRequest;
import com.carolinysilva.taskflow_backend.dto.LoginResponse;
import com.carolinysilva.taskflow_backend.dto.RefreshResponse;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
import com.carolinysilva.taskflow_backend.dto.UserResponse;
import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.RateLimitExceededException;
import com.carolinysilva.taskflow_backend.security.RateLimiter;
import com.carolinysilva.taskflow_backend.service.AuthService;
import com.carolinysilva.taskflow_backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserService userService;
    private final AuthService authService;
    private final RateLimiter rateLimiter;
    private final long refreshTokenExpirationMs;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final int maxRegisterAttempts;
    private final Duration registerAttemptWindow;

    public AuthController(
            UserService userService,
            AuthService authService,
            RateLimiter rateLimiter,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${jwt.refresh-cookie-secure}") boolean refreshCookieSecure,
            @Value("${jwt.refresh-cookie-same-site}") String refreshCookieSameSite,
            @Value("${rate-limit.register.max-attempts}") int maxRegisterAttempts,
            @Value("${rate-limit.register.window-minutes}") long registerAttemptWindowMinutes) {
        this.userService = userService;
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.maxRegisterAttempts = maxRegisterAttempts;
        this.registerAttemptWindow = Duration.ofMinutes(registerAttemptWindowMinutes);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String rateLimitKey = "register:" + clientKey(httpRequest);
        if (rateLimiter.isBlocked(rateLimitKey, maxRegisterAttempts, registerAttemptWindow)) {
            throw new RateLimitExceededException(
                    "TOO_MANY_ATTEMPTS", "Muitas tentativas de cadastro. Tente novamente mais tarde");
        }
        rateLimiter.recordAttempt(rateLimitKey, registerAttemptWindow);

        User user = userService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthService.TokenPair tokenPair = authService.login(request.email(), request.password(), clientKey(httpRequest));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken(), refreshTokenExpirationMs).toString())
                .body(new LoginResponse(tokenPair.accessToken(), UserResponse.from(tokenPair.user())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthService.TokenPair tokenPair = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(tokenPair.refreshToken(), refreshTokenExpirationMs).toString())
                .body(new RefreshResponse(tokenPair.accessToken(), UserResponse.from(tokenPair.user())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        authService.logout(extractBearerToken(authorizationHeader), refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
                .build();
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, long maxAgeMs) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ofMillis(maxAgeMs))
                .build();
    }
}
