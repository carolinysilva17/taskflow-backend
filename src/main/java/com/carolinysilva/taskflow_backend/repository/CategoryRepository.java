package com.carolinysilva.taskflow_backend.repository;

import com.carolinysilva.taskflow_backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserId(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndColorIgnoreCase(Long userId, String color);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);

    boolean existsByUserIdAndColorIgnoreCaseAndIdNot(Long userId, String color, Long id);

    // TODO: substituir por TaskRepository.existsByCategoryId quando a entidade Task existir
    @Query(value = "SELECT EXISTS (SELECT 1 FROM tasks WHERE category_id = :categoryId)", nativeQuery = true)
    boolean existsTaskWithCategoryId(@Param("categoryId") Long categoryId);
}
