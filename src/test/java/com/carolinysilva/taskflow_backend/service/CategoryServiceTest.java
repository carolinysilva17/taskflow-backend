package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.dto.CategoryRequest;
import com.carolinysilva.taskflow_backend.dto.CategoryResponse;
import com.carolinysilva.taskflow_backend.entity.Category;
import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.BusinessRuleException;
import com.carolinysilva.taskflow_backend.exception.ResourceNotFoundException;
import com.carolinysilva.taskflow_backend.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    private CategoryService categoryService;

    private User user;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, userService);
        user = new User("Carol", "carol@test.com", "hash");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void listByUser_shouldReturnCategoriesOfAuthenticatedUser() {
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.findByUserId(1L))
                .thenReturn(List.of(new Category("Trabalho", "#4CAF50", user)));

        List<CategoryResponse> result = categoryService.listByUser("carol@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Trabalho");
    }

    @Test
    void create_shouldSaveCategoryLinkedToAuthenticatedUser_andTrimName() {
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Trabalho")).thenReturn(false);
        when(categoryRepository.existsByUserIdAndColorIgnoreCase(1L, "#4CAF50")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.create("carol@test.com", new CategoryRequest("  Trabalho  ", "#4CAF50"));

        assertThat(result.name()).isEqualTo("Trabalho");
        assertThat(result.color()).isEqualTo("#4CAF50");

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    void create_shouldThrowException_whenNameIsDuplicatedForSameUser() {
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Trabalho")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create("carol@test.com", new CategoryRequest("Trabalho", "#4CAF50")))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowException_whenColorIsDuplicatedForSameUser() {
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Trabalho")).thenReturn(false);
        when(categoryRepository.existsByUserIdAndColorIgnoreCase(1L, "#4CAF50")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create("carol@test.com", new CategoryRequest("Trabalho", "#4CAF50")))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateNameAndColor_whenCategoryBelongsToUser() {
        Category category = new Category("Trabalho", "#4CAF50", user);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(1L, "Estudos", 10L)).thenReturn(false);
        when(categoryRepository.existsByUserIdAndColorIgnoreCaseAndIdNot(1L, "#2196F3", 10L)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.update("carol@test.com", 10L, new CategoryRequest("Estudos", "#2196F3"));

        assertThat(result.name()).isEqualTo("Estudos");
        assertThat(result.color()).isEqualTo("#2196F3");
    }

    @Test
    void update_shouldThrowResourceNotFound_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update("carol@test.com", 10L, new CategoryRequest("Estudos", "#2196F3")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowResourceNotFound_whenCategoryBelongsToAnotherUser() {
        User otherUser = new User("Outro", "outro@test.com", "hash");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        Category category = new Category("Trabalho", "#4CAF50", otherUser);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);

        assertThatThrownBy(() -> categoryService.update("carol@test.com", 10L, new CategoryRequest("Estudos", "#2196F3")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowException_whenNameIsDuplicatedForSameUser() {
        Category category = new Category("Trabalho", "#4CAF50", user);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(1L, "Estudos", 10L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update("carol@test.com", 10L, new CategoryRequest("Estudos", "#2196F3")))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowException_whenColorIsDuplicatedForSameUser() {
        Category category = new Category("Trabalho", "#4CAF50", user);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(1L, "Estudos", 10L)).thenReturn(false);
        when(categoryRepository.existsByUserIdAndColorIgnoreCaseAndIdNot(1L, "#2196F3", 10L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update("carol@test.com", 10L, new CategoryRequest("Estudos", "#2196F3")))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void delete_shouldRemoveCategory_whenItHasNoTasksLinked() {
        Category category = new Category("Trabalho", "#4CAF50", user);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsTaskWithCategoryId(10L)).thenReturn(false);

        categoryService.delete("carol@test.com", 10L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void delete_shouldThrowException_whenCategoryHasTasksLinked() {
        Category category = new Category("Trabalho", "#4CAF50", user);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);
        when(categoryRepository.existsTaskWithCategoryId(10L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete("carol@test.com", 10L))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowResourceNotFound_whenCategoryBelongsToAnotherUser() {
        User otherUser = new User("Outro", "outro@test.com", "hash");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        Category category = new Category("Trabalho", "#4CAF50", otherUser);
        ReflectionTestUtils.setField(category, "id", 10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(userService.findByEmail("carol@test.com")).thenReturn(user);

        assertThatThrownBy(() -> categoryService.delete("carol@test.com", 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }
}
