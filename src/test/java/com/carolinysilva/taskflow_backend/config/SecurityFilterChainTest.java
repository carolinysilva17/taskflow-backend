package com.carolinysilva.taskflow_backend.config;

import com.carolinysilva.taskflow_backend.dto.LoginRequest;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'security-test%'");
    }

    @Test
    void protectedRoute_shouldReturn401_whenRefreshTokenIsUsedAsAccessToken() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "security-test-token-type@test.com", "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("security-test-token-type@test.com", "strongPassword123"));
        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + refreshCookie.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/any-protected-route"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authRoute_shouldBeAccessible_withoutToken() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }

    @Test
    void cors_shouldAllowConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void cors_shouldRejectNonConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://unauthorized-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_shouldNotAllowNonConfiguredOrigin_onActualRequest() throws Exception {
        mockMvc.perform(get("/auth/register").header("Origin", "http://unauthorized-site.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
