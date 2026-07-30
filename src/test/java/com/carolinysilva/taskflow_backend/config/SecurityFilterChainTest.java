package com.carolinysilva.taskflow_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

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
