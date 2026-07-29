package com.carolinysilva.taskflow_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.carolinysilva.taskflow_backend.dto.CategoryRequest;
import com.carolinysilva.taskflow_backend.dto.CategoryResponse;
import com.carolinysilva.taskflow_backend.dto.MessageResponse;
import com.carolinysilva.taskflow_backend.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(categoryService.listByUser(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        CategoryResponse category = categoryService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request, Authentication authentication) {
        CategoryResponse category = categoryService.update(authentication.getName(), id, request);
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id, Authentication authentication) {
        categoryService.delete(authentication.getName(), id);
        return ResponseEntity.ok(new MessageResponse("Categoria excluída com sucesso"));
    }
}
