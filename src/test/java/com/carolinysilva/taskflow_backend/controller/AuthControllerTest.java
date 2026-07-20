package com.carolinysilva.taskflow_backend.controller;

import com.carolinysilva.taskflow_backend.dto.LoginRequest;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
import com.carolinysilva.taskflow_backend.security.JwtService;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'auth-test%'");
    }

    @Test
    void register_shouldReturn201WithUserWithoutPassword_whenDataIsValid() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-success@test.com", "strongPassword123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("auth-test-success@test.com"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_shouldReturnError_whenEmailIsDuplicated() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-duplicate@test.com", "strongPassword123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_IN_USE"));
    }

    @Test
    void register_shouldReturn400_whenFieldsAreInvalid() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("", "invalid-email", "123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnAccessTokenInBodyAndRefreshTokenInCookie_whenCredentialsAreValid() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-login@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-login@test.com", "strongPassword123"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value("auth-test-login@test.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void login_shouldReturn401_whenPasswordIsIncorrect() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-wrong-password@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-wrong-password@test.com", "wrongPassword"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn401_whenEmailDoesNotExist() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-does-not-exist@test.com", "anyPassword"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturnNewAccessToken_whenRefreshTokenIsValid() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-refresh@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-refresh@test.com", "strongPassword123"));
        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void refresh_shouldReturnNewRefreshTokenDifferentFromPrevious_whenRefreshTokenIsValid() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-rotation@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-rotation@test.com", "strongPassword123"));
        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie originalRefreshCookie = loginResult.getResponse().getCookie("refreshToken");

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh").cookie(originalRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        Cookie newRefreshCookie = refreshResult.getResponse().getCookie("refreshToken");

        org.junit.jupiter.api.Assertions.assertNotEquals(
                originalRefreshCookie.getValue(), newRefreshCookie.getValue());
    }

    @Test
    void refresh_shouldReturn401_whenAccessTokenIsUsedInsteadOfRefreshToken() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-token-type@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-token-type@test.com", "strongPassword123"));
        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asString();

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn401_whenNoCookie() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn401_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "completely-invalid-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn401_whenTokenIsExpired() throws Exception {
        JwtService jwtServiceWithShortExpiration = new JwtService(
                "test-secret-key-with-at-least-32-characters-long", -1000L, -1000L);
        String expiredToken = jwtServiceWithShortExpiration.generateRefreshToken("auth-test-refresh@test.com");

        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_shouldClearRefreshTokenCookie_evenWithoutCookiePresent() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
