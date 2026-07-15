package com.carolinysilva.taskflow_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rotaProtegida_deveRetornar401_semAutenticacao() throws Exception {
        mockMvc.perform(get("/qualquer-rota-protegida"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaAuth_deveSerAcessivel_semToken() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().is(org.hamcrest.Matchers.not(401)));
    }
}
