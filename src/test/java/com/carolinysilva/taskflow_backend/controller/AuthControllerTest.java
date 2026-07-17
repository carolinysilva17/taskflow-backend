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
import org.springframework.test.web.servlet.MockMvc;

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
    void register_deveRetornar201ComUsuarioSemSenha_quandoDadosValidos() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-sucesso@teste.com", "senhaForte123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("auth-test-sucesso@teste.com"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_deveRetornarErro_quandoEmailDuplicado() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-duplicado@teste.com", "senhaForte123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void register_deveRetornar400_quandoCamposInvalidos() throws Exception {
        String body = objectMapper.writeValueAsString(
                new RegisterRequest("", "email-invalido", "123"));

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_deveRetornarAccessTokenNoCorpoERefreshTokenNoCookie_quandoCredenciaisValidas() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-login@teste.com", "senhaForte123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-login@teste.com", "senhaForte123"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.user.email").value("auth-test-login@teste.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void login_deveRetornar401_quandoSenhaIncorreta() throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", "auth-test-senha-errada@teste.com", "senhaForte123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-senha-errada@teste.com", "senhaErrada"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_deveRetornar401_quandoEmailNaoExiste() throws Exception {
        String loginBody = objectMapper.writeValueAsString(
                new LoginRequest("auth-test-nao-existe@teste.com", "qualquerSenha"));

        mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isUnauthorized());
    }
}
