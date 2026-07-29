package com.carolinysilva.taskflow_backend.controller;

import com.carolinysilva.taskflow_backend.dto.CategoryRequest;
import com.carolinysilva.taskflow_backend.dto.RegisterRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM categories WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'category-test%')");
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE 'category-test%'");
    }

    private String registerAndLogin(String email) throws Exception {
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Carol", email, "strongPassword123"));
        mockMvc.perform(post("/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                new com.carolinysilva.taskflow_backend.dto.LoginRequest(email, "strongPassword123"));
        MvcResult loginResult = mockMvc.perform(post("/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("accessToken").asString();
    }

    private Long createCategory(String token, String name, String color) throws Exception {
        String body = objectMapper.writeValueAsString(new CategoryRequest(name, color));
        MvcResult result = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).get("id").asLong();
    }

    @Test
    void list_shouldReturnOnlyCategoriesFromAuthenticatedUser() throws Exception {
        String tokenUserA = registerAndLogin("category-test-list-a@test.com");
        String tokenUserB = registerAndLogin("category-test-list-b@test.com");

        createCategory(tokenUserA, "Trabalho", "#4CAF50");
        createCategory(tokenUserB, "Pessoal", "#2196F3");

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Trabalho"));
    }

    @Test
    void list_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturn201WithCategory_whenDataIsValid() throws Exception {
        String token = registerAndLogin("category-test-create@test.com");
        String body = objectMapper.writeValueAsString(new CategoryRequest("Estudos", "#FF9800"));

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Estudos"))
                .andExpect(jsonPath("$.color").value("#FF9800"));
    }

    @Test
    void create_shouldReturn400_whenFieldsAreInvalid() throws Exception {
        String token = registerAndLogin("category-test-create-invalid@test.com");
        String body = objectMapper.writeValueAsString(new CategoryRequest("", "not-a-color"));

        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn409_whenNameIsDuplicatedForSameUser() throws Exception {
        String token = registerAndLogin("category-test-dup-name@test.com");
        createCategory(token, "Trabalho", "#4CAF50");

        String body = objectMapper.writeValueAsString(new CategoryRequest("Trabalho", "#2196F3"));
        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NAME_DUPLICATE"));
    }

    @Test
    void create_shouldReturn409_whenColorIsDuplicatedForSameUser() throws Exception {
        String token = registerAndLogin("category-test-dup-color@test.com");
        createCategory(token, "Trabalho", "#4CAF50");

        String body = objectMapper.writeValueAsString(new CategoryRequest("Pessoal", "#4CAF50"));
        mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_COLOR_DUPLICATE"));
    }

    @Test
    void update_shouldReturn200WithUpdatedCategory_whenDataIsValid() throws Exception {
        String token = registerAndLogin("category-test-update@test.com");
        Long categoryId = createCategory(token, "Trabalho", "#4CAF50");

        String body = objectMapper.writeValueAsString(new CategoryRequest("Trabalho Atualizado", "#2196F3"));
        mockMvc.perform(put("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Trabalho Atualizado"))
                .andExpect(jsonPath("$.color").value("#2196F3"));
    }

    @Test
    void update_shouldReturn404_whenCategoryDoesNotBelongToUser() throws Exception {
        String tokenOwner = registerAndLogin("category-test-update-owner@test.com");
        String tokenOther = registerAndLogin("category-test-update-other@test.com");
        Long categoryId = createCategory(tokenOwner, "Trabalho", "#4CAF50");

        String body = objectMapper.writeValueAsString(new CategoryRequest("Hack", "#2196F3"));
        mockMvc.perform(put("/categories/" + categoryId)
                        .header("Authorization", "Bearer " + tokenOther)
                        .contentType("application/json").content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void update_shouldReturn404_whenCategoryDoesNotExist() throws Exception {
        String token = registerAndLogin("category-test-update-missing@test.com");

        String body = objectMapper.writeValueAsString(new CategoryRequest("Trabalho", "#4CAF50"));
        mockMvc.perform(put("/categories/999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturn200WithMessage_whenCategoryHasNoTasks() throws Exception {
        String token = registerAndLogin("category-test-delete@test.com");
        Long categoryId = createCategory(token, "Trabalho", "#4CAF50");

        mockMvc.perform(delete("/categories/" + categoryId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void delete_shouldReturn404_whenCategoryDoesNotBelongToUser() throws Exception {
        String tokenOwner = registerAndLogin("category-test-delete-owner@test.com");
        String tokenOther = registerAndLogin("category-test-delete-other@test.com");
        Long categoryId = createCategory(tokenOwner, "Trabalho", "#4CAF50");

        mockMvc.perform(delete("/categories/" + categoryId).header("Authorization", "Bearer " + tokenOther))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturn404_whenCategoryDoesNotExist() throws Exception {
        String token = registerAndLogin("category-test-delete-missing@test.com");

        mockMvc.perform(delete("/categories/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
    }
}
