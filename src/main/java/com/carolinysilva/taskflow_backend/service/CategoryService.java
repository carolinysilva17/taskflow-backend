package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.dto.CategoryRequest;
import com.carolinysilva.taskflow_backend.dto.CategoryResponse;
import com.carolinysilva.taskflow_backend.entity.Category;
import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.BusinessRuleException;
import com.carolinysilva.taskflow_backend.exception.ResourceNotFoundException;
import com.carolinysilva.taskflow_backend.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryService(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    public List<CategoryResponse> listByUser(String userEmail) {
        User user = userService.findByEmail(userEmail);
        return categoryRepository.findByUserId(user.getId()).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse create(String userEmail, CategoryRequest request) {
        User user = userService.findByEmail(userEmail);
        Category category = new Category(request.name().trim(), request.color(), user);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse update(String userEmail, Long categoryId, CategoryRequest request) {
        Category category = getOwnedCategory(userEmail, categoryId);
        category.setName(request.name().trim());
        category.setColor(request.color());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(String userEmail, Long categoryId) {
        Category category = getOwnedCategory(userEmail, categoryId);

        if (categoryRepository.existsTaskWithCategoryId(category.getId())) {
            throw new BusinessRuleException(
                    "CATEGORY_HAS_TASKS", "Categoria possui tarefas vinculadas e não pode ser excluída");
        }

        categoryRepository.delete(category);
    }

    private Category getOwnedCategory(String userEmail, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "Categoria não encontrada"));

        User user = userService.findByEmail(userEmail);
        if (!category.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("CATEGORY_NOT_FOUND", "Categoria não encontrada");
        }

        return category;
    }
}
