package com.carolinysilva.taskflow_backend.controller;

import com.carolinysilva.taskflow_backend.dto.LoginRequest;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "rate-limit.login.max-attempts=3",
        "rate-limit.login.window-minutes=15",
        "rate-limit.register.max-attempts=2",
        "rate-limit.register.window-minutes=60",
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthRateLimitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'rate-limit-test%'");
    }

    @Test
    void login_shouldReturn429_afterExceedingMaxFailedAttempts() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "rate-limit-test-login@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String wrongLoginBody = objectMapper.writeValueAsString(
                new LoginRequest("rate-limit-test-login@test.com", "wrongPassword"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login").contentType("application/json").content(wrongLoginBody))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login").contentType("application/json").content(wrongLoginBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void login_shouldStillAllowCorrectPassword_beforeReachingTheLimit() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "rate-limit-test-ok@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String wrongLoginBody = objectMapper.writeValueAsString(
                new LoginRequest("rate-limit-test-ok@test.com", "wrongPassword"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(wrongLoginBody))
                .andExpect(status().isUnauthorized());

        String correctLoginBody = objectMapper.writeValueAsString(
                new LoginRequest("rate-limit-test-ok@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/login").contentType("application/json").content(correctLoginBody))
                .andExpect(status().isOk());
    }

    @Test
    void register_shouldReturn429_afterExceedingMaxAttemptsFromSameClient() throws Exception {
        for (int i = 0; i < 2; i++) {
            String body = objectMapper.writeValueAsString(
                    new RegisterRequest("Carol", "rate-limit-test-register-" + i + "@test.com", "strongPassword123"));
            mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                    .andExpect(status().isCreated());
        }

        String body = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "rate-limit-test-register-blocked@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("TOO_MANY_ATTEMPTS"));
    }
}
